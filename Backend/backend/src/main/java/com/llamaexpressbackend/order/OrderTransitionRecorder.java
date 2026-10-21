package com.llamaexpressbackend.order;

import com.llamaexpressbackend.common.outbox.OutboxService;
import com.llamaexpressbackend.order.event.OrderCreatedEvent;
import com.llamaexpressbackend.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Setiap perubahan status order WAJIB lewat sini: riwayat + event outbox, dalam
 * transaksi yang sama
 */
@Component
@RequiredArgsConstructor
public class OrderTransitionRecorder {

    private static final String AGGREGATE = "Order";

    private final OrderStatusHistoryRepository historyRepository;
    private final OutboxService outboxService;
    private final Clock clock;

    private final OrderMetrics metrics; // field baru (constructor dibuat otomatis oleh Lombok)

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordCreated(DeliveryOrder order) {
        outboxService.record(AGGREGATE, order.getOrderCode(), OrderCreatedEvent.TYPE,
                OrderCreatedEvent.from(order, clock.instant()));
        metrics.created(order); // ⬅ baru
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(DeliveryOrder order, OrderStatus from, Long actorId, String note) {
        Instant now = clock.instant();
        historyRepository.save(OrderStatusHistory.record(order, from, actorId, note, now));
        if (from != null) {
            outboxService.record(AGGREGATE, order.getOrderCode(), OrderStatusChangedEvent.TYPE,
                    new OrderStatusChangedEvent(order.getOrderCode(), from, order.getStatus(), actorId, note, now));
        }
        metrics.transitioned(order); // ⬅ baru
    }
}