package com.llamaexpressbackend.dispatch;

import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.courier.CourierLocationStore;
import com.llamaexpressbackend.courier.CourierProfile;
import com.llamaexpressbackend.courier.CourierProfileRepository;
import com.llamaexpressbackend.dispatch.dto.CourierJobResponse;
import com.llamaexpressbackend.dispatch.dto.OfferResponse;
import com.llamaexpressbackend.order.*;
import com.llamaexpressbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OfferService {

    private static final Set<OrderStatus> ACTIVE_JOB = EnumSet.of(
            OrderStatus.ASSIGNED, OrderStatus.PICKING_UP, OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT);

    private final OrderOfferRepository offerRepository;
    private final DeliveryOrderRepository orderRepository;
    private final CourierProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final CourierLocationStore locationStore;
    private final OrderTransitionRecorder transitions;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<OfferResponse> activeOffers(Long courierId) {
        return offerRepository.findActiveForCourier(courierId, OfferStatus.OFFERED, clock.instant())
                .stream().map(OfferResponse::from).toList();
    }

    @Transactional
    public CourierJobResponse accept(Long courierId, Long offerId) {
        Instant now = clock.instant();
        OrderOffer offer = findOwn(courierId, offerId);
        offer.ensureAcceptable(now); // 1. validasi saja, belum mengubah apa-apa

        CourierProfile profile = profileRepository.findById(courierId)
                .orElseThrow(() -> new NotFoundException("COURIER_NOT_FOUND", "Profil kurir tidak ditemukan"));
        if (!profile.isOnline()) {
            throw new ConflictException("COURIER_OFFLINE", "Aktifkan status online terlebih dahulu");
        }
        if (orderRepository.existsByCourier_IdAndStatusIn(courierId, ACTIVE_JOB)) {
            throw new ConflictException("COURIER_BUSY", "Selesaikan pengantaran yang sedang berjalan terlebih dahulu");
        }

        // 2. KUNCI baris ORDER lebih dulu. Semua kurir yang menerima bersamaan "antre"
        // di sini:
        // yang pertama menang, sisanya gagal di cek @Version → 409. Urutan ini mencegah
        // deadlock.
        DeliveryOrder order = offer.getOrder();
        OrderStatus from = order.getStatus();
        order.assignCourier(userRepository.getReferenceById(courierId), now);
        try {
            orderRepository.saveAndFlush(order);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("ORDER_ALREADY_TAKEN", "Order sudah diambil kurir lain");
        }

        // 3. Baru setelah itu sentuh baris tawaran
        offer.markAccepted(now);
        transitions.record(order, from, courierId, "Diterima kurir");
        offerRepository.expireOthers(order.getId(), offer.getId(), now, OfferStatus.OFFERED, OfferStatus.EXPIRED);

        // 4. Redis paling akhir — setelah semua operasi DB yang bisa gagal
        locationStore.markBusy(courierId, profile.getVehicleType());
        return CourierJobResponse.from(order);
    }

    @Transactional
    public void reject(Long courierId, Long offerId) {
        findOwn(courierId, offerId).reject(clock.instant());
        // putaran berikutnya diurus DispatchSweeper
    }

    private OrderOffer findOwn(Long courierId, Long offerId) {
        return offerRepository.findByIdAndCourierId(offerId, courierId)
                .orElseThrow(() -> new NotFoundException("OFFER_NOT_FOUND", "Tawaran tidak ditemukan"));
    }
}