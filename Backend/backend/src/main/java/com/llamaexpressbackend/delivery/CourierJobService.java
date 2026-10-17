package com.llamaexpressbackend.delivery;

import com.llamaexpressbackend.common.error.BadRequestException;
import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.courier.CourierLocationStore;
import com.llamaexpressbackend.dispatch.dto.CourierJobResponse;
import com.llamaexpressbackend.order.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourierJobService {

    private final DeliveryOrderRepository orderRepository;
    private final ProofOfDeliveryRepository proofRepository;
    private final OrderTransitionRecorder transitions;
    private final CourierLocationStore locationStore;
    private final VerificationAttemptGuard attemptGuard;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Optional<CourierJobResponse> activeJob(Long courierId) {
        return orderRepository.findFirstByCourier_IdAndStatusIn(courierId, OrderStatus.COURIER_ACTIVE)
                .map(CourierJobResponse::from);
    }

    @Transactional
    public CourierJobResponse startPickup(Long courierId, String code) {
        DeliveryOrder order = findAssigned(courierId, code);
        OrderStatus from = order.getStatus();
        order.startPickup();
        transitions.record(order, from, courierId, "Kurir menuju lokasi jemput");
        return CourierJobResponse.from(order);
    }

    /**
     * Satu tombol "Paket sudah diambil" = dua transisi: PICKED_UP lalu IN_TRANSIT
     */
    @Transactional
    public CourierJobResponse pickUp(Long courierId, String code) {
        DeliveryOrder order = findAssigned(courierId, code);
        OrderStatus from = order.getStatus();
        order.pickUp(clock.instant());
        transitions.record(order, from, courierId, "Paket sudah diambil");

        order.depart();
        transitions.record(order, OrderStatus.PICKED_UP, courierId, "Dalam perjalanan ke penerima");
        return CourierJobResponse.from(order);
    }

    @Transactional
    public CourierJobResponse complete(Long courierId, String code, String verificationCode) {
        attemptGuard.ensureNotLocked(code);
        DeliveryOrder order = findAssigned(courierId, code);

        if (order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new ConflictException("NOT_IN_TRANSIT", "Paket belum dalam perjalanan");
        }
        if (!proofRepository.existsByOrder_Id(order.getId())) {
            throw new ConflictException("PROOF_REQUIRED", "Unggah foto bukti pengiriman terlebih dahulu");
        }
        if (!constantTimeEquals(order.getVerificationCode(), verificationCode)) {
            attemptGuard.recordFailure(code);
            throw new BadRequestException("INVALID_VERIFICATION_CODE", "Kode verifikasi salah");
        }

        attemptGuard.reset(code);
        Instant now = clock.instant();
        OrderStatus from = order.getStatus();
        order.deliver(now);
        transitions.record(order, from, courierId, "Paket diterima");
        locationStore.markAvailable(courierId);
        return CourierJobResponse.from(order);
    }

    private DeliveryOrder findAssigned(Long courierId, String code) {
        return orderRepository.findByOrderCodeAndCourier_Id(code, courierId)
                .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Pengantaran tidak ditemukan"));
    }

    /** Waktu perbandingan tidak bergantung pada posisi digit yang salah */
    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}