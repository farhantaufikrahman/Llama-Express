package com.llamaexpressbackend.dispatch;

import com.llamaexpressbackend.common.kafka.ProcessedEventStore;
import com.llamaexpressbackend.courier.CourierLocationStore;
import com.llamaexpressbackend.courier.NearbyCourier;
import com.llamaexpressbackend.order.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final DeliveryOrderRepository orderRepository;
    private final OrderOfferRepository offerRepository;
    private final CourierLocationStore locationStore;
    private final OrderTransitionRecorder transitions;
    private final ProcessedEventStore processedEvents;
    private final DispatchProperties props;
    private final Clock clock;

    /** Dipanggil dari Kafka listener — idempotent berdasarkan eventId */
    @Transactional
    public void dispatchFromEvent(String consumer, String eventId, String orderCode) {
        if (!processedEvents.markProcessed(consumer, eventId)) {
            log.debug("Event {} sudah pernah diproses — dilewati", eventId);
            return;
        }
        dispatchRound(orderCode);
    }

    /**
     * Satu putaran dispatch. Aman dipanggil berulang (dari listener maupun
     * sweeper).
     */
    @Transactional
    public void dispatchRound(String orderCode) {
        DeliveryOrder order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.SEARCHING_COURIER) {
            return;
        }
        Instant now = clock.instant();
        if (offerRepository.existsByOrder_IdAndStatusAndExpiresAtAfter(order.getId(), OfferStatus.OFFERED, now)) {
            return; // masih ada tawaran yang menunggu jawaban
        }
        if (order.getDispatchAttempts() >= props.maxAttempts()) {
            OrderStatus from = order.getStatus();
            order.failDispatch();
            transitions.record(order, from, null, "Tidak ada kurir yang tersedia");
            log.info("Dispatch {} gagal setelah {} putaran", orderCode, order.getDispatchAttempts());
            return;
        }

        order.incrementDispatchAttempts();
        Set<Long> alreadyOffered = offerRepository.findCourierIdsByOrderId(order.getId());
        List<NearbyCourier> candidates = locationStore.findNearby(order.getVehicleType(),
                order.getPickup().getLat().doubleValue(), order.getPickup().getLng().doubleValue(),
                props.radiusKm(), props.candidatesPerRound() + alreadyOffered.size())
                .stream()
                .filter(c -> !alreadyOffered.contains(c.courierId()))
                .limit(props.candidatesPerRound())
                .toList();

        Instant expiresAt = now.plusSeconds(props.offerTtlSeconds());
        for (NearbyCourier c : candidates) {
            offerRepository.save(OrderOffer.create(order, c.courierId(), now, expiresAt));
        }
        log.info("Dispatch {} putaran {}: {} kurir ditawari", orderCode, order.getDispatchAttempts(),
                candidates.size());
    }

    @Transactional
    public int expireStaleOffers() {
        return offerRepository.expireStale(clock.instant(), OfferStatus.OFFERED, OfferStatus.EXPIRED);
    }
}