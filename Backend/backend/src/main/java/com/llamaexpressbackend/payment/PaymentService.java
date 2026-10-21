package com.llamaexpressbackend.payment;

import com.llamaexpressbackend.common.error.BadRequestException;
import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.ForbiddenException;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.order.*;
import com.llamaexpressbackend.payment.dto.PaymentResponse;
import com.llamaexpressbackend.payment.gateway.MidtransNotification;
import com.llamaexpressbackend.payment.gateway.PaymentGateway;
import com.llamaexpressbackend.payment.gateway.SnapRequest;
import com.llamaexpressbackend.payment.gateway.SnapResponse;
import com.llamaexpressbackend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final DeliveryOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway gateway;
    private final MidtransSignatureVerifier signatureVerifier;
    private final MidtransProperties props;
    private final OrderTransitionRecorder transitions;
    private final TransactionTemplate tx;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    // ================== 1. Membuat transaksi Snap ==================

    /**
     * Tanpa @Transactional: panggilan HTTP ke Midtrans tidak boleh menahan koneksi
     * DB (pola langkah 7A)
     */
    public PaymentResponse createPayment(Long customerId, String code) {
        // Fase 1 — cek & rencanakan
        PaymentPlan plan = tx.execute(s -> {
            DeliveryOrder order = findOwned(customerId, code);
            if (order.getPaymentMethod() != PaymentMethod.MIDTRANS) {
                throw new BadRequestException("NOT_MIDTRANS_ORDER", "Order ini dibayar tunai ke kurir");
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                throw new ConflictException("ALREADY_PAID", "Order ini sudah dibayar");
            }
            if (order.getStatus() != OrderStatus.CREATED) {
                throw new ConflictException("ORDER_NOT_PAYABLE",
                        "Order dengan status " + order.getStatus() + " tidak bisa dibayar");
            }
            Optional<Payment> reusable = paymentRepository
                    .findFirstByOrder_IdAndStatusOrderByIdDesc(order.getId(), PaymentTransactionStatus.PENDING)
                    .filter(p -> p.isReusable(clock.instant()));
            if (reusable.isPresent()) {
                return PaymentPlan.reuse(PaymentResponse.from(reusable.get())); // idempotent: klik "Bayar" 2x → token
                                                                                // sama
            }
            long attempt = paymentRepository.countByOrder_Id(order.getId()) + 1;
            User customer = order.getCustomer();
            return new PaymentPlan(null, code + "-P" + attempt, order.getPriceTotal(),
                    "Llama Express " + order.getServiceType() + " " + code,
                    customer.getFullName(), customer.getEmail(), customer.getPhone());
        });
        if (plan.existing() != null) {
            return plan.existing();
        }

        // Fase 2 — panggil Midtrans (tanpa transaksi)
        long gross = plan.amount().setScale(0, RoundingMode.UNNECESSARY).longValueExact(); // harga kita selalu
                                                                                           // kelipatan Rp500
        SnapResponse snap = gateway.createTransaction(new SnapRequest(
                new SnapRequest.TransactionDetails(plan.merchantOrderId(), gross),
                List.of(new SnapRequest.ItemDetail(code, gross, 1, plan.itemName())),
                new SnapRequest.CustomerDetails(plan.customerName(), plan.customerEmail(), plan.customerPhone()),
                new SnapRequest.Expiry("minute", props.expiryMinutes())));

        // Fase 3 — simpan
        return tx.execute(s -> {
            DeliveryOrder order = findOwned(customerId, code);
            Instant expiresAt = clock.instant().plusSeconds(props.expiryMinutes() * 60L);
            Payment payment = paymentRepository.save(Payment.pending(order, plan.merchantOrderId(), plan.amount(),
                    snap.token(), snap.redirectUrl(), expiresAt));
            order.markPaymentPending();
            return PaymentResponse.from(payment);
        });
    }

    // ================== 2. Webhook dari Midtrans ==================

    public void handleNotification(String rawBody) {
        MidtransNotification notification;
        try {
            notification = jsonMapper.readValue(rawBody, MidtransNotification.class);
        } catch (JacksonException e) {
            throw new BadRequestException("INVALID_PAYLOAD", "Format notifikasi tidak valid");
        }
        if (!signatureVerifier.isValid(notification)) {
            log.warn("Notifikasi Midtrans dengan signature TIDAK valid untuk order_id={}", notification.orderId());
            throw new ForbiddenException("INVALID_SIGNATURE", "Signature tidak valid");
        }
        tx.executeWithoutResult(s -> apply(notification, rawBody));
    }

    // ================== 3. Sinkronisasi manual (dev lokal / rekonsiliasi)
    // ==================

    public PaymentResponse sync(Long customerId, String code) {
        String merchantOrderId = tx.execute(s -> latestPayment(customerId, code).getMerchantOrderId());
        // Data dari Status API kita ambil SENDIRI dengan Server Key → tidak perlu cek
        // signature
        MidtransNotification status = gateway.getStatus(merchantOrderId);
        return tx.execute(s -> {
            apply(status, jsonMapper.writeValueAsString(status));
            return PaymentResponse.from(latestPayment(customerId, code));
        });
    }

    // ================== Inti: menerapkan status dari Midtrans ==================

    private void apply(MidtransNotification n, String raw) {
        Payment payment = paymentRepository.findForUpdateByMerchantOrderId(n.orderId()).orElse(null);
        if (payment == null) {
            log.warn("Notifikasi untuk order_id yang tidak dikenal: {}", n.orderId());
            return; // tetap 200 supaya Midtrans tidak mengulang terus
        }
        if (!amountMatches(payment, n.grossAmount())) {
            log.error("Nominal tidak cocok untuk {}: tercatat {}, notifikasi {}", n.orderId(), payment.getAmount(),
                    n.grossAmount());
            return;
        }
        PaymentTransactionStatus newStatus = map(n);
        if (newStatus == null) {
            return; // mis. "deny": pelanggan masih bisa mencoba metode lain di halaman Snap yang
                    // sama
        }
        if (payment.getStatus() == PaymentTransactionStatus.PAID && newStatus != PaymentTransactionStatus.REFUNDED) {
            return; // sudah lunas → abaikan notifikasi duplikat / yang datang terlambat
        }

        payment.apply(newStatus, n.transactionId(), n.paymentType(), raw, clock.instant());
        DeliveryOrder order = payment.getOrder();
        switch (newStatus) {
            case PAID -> onPaid(order);
            case FAILED, EXPIRED -> order.markPaymentFailed();
            case REFUNDED -> order.markRefunded();
            case PENDING -> order.markPaymentPending();
        }
    }

    private void onPaid(DeliveryOrder order) {
        order.markPaid();
        if (order.getStatus() == OrderStatus.CREATED) {
            OrderStatus from = order.getStatus();
            order.startSearchingCourier(); // → outbox → Kafka → dispatch (jalur langkah 6B, tanpa kode baru)
            transitions.record(order, from, null, "Pembayaran diterima");
        } else {
            // contoh: customer membatalkan order, lalu pembayarannya tetap masuk
            log.warn("Order {} berstatus {} saat pembayaran masuk — PERLU REFUND MANUAL", order.getOrderCode(),
                    order.getStatus());
        }
    }

    /** Pemetaan status Midtrans → status kita (mengikuti panduan Midtrans) */
    private static PaymentTransactionStatus map(MidtransNotification n) {
        if (n.transactionStatus() == null) {
            return null;
        }
        return switch (n.transactionStatus()) {
            case "settlement" -> PaymentTransactionStatus.PAID;
            case "capture" ->
                "accept".equals(n.fraudStatus()) ? PaymentTransactionStatus.PAID : PaymentTransactionStatus.PENDING;
            case "pending" -> PaymentTransactionStatus.PENDING;
            case "cancel", "failure" -> PaymentTransactionStatus.FAILED;
            case "expire" -> PaymentTransactionStatus.EXPIRED;
            case "refund", "partial_refund" -> PaymentTransactionStatus.REFUNDED;
            default -> null; // "deny" dan status lain: tidak mengubah apa pun
        };
    }

    private static boolean amountMatches(Payment payment, String grossAmount) {
        try {
            return grossAmount != null && new BigDecimal(grossAmount).compareTo(payment.getAmount()) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private DeliveryOrder findOwned(Long customerId, String code) {
        return orderRepository.findByOrderCodeAndCustomer_Id(code, customerId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order tidak ditemukan"));
    }

    private Payment latestPayment(Long customerId, String code) {
        DeliveryOrder order = findOwned(customerId, code);
        return paymentRepository.findFirstByOrder_IdOrderByIdDesc(order.getId())
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Belum ada transaksi pembayaran"));
    }

    private record PaymentPlan(PaymentResponse existing, String merchantOrderId, BigDecimal amount, String itemName,
            String customerName, String customerEmail, String customerPhone) {
        static PaymentPlan reuse(PaymentResponse existing) {
            return new PaymentPlan(existing, null, null, null, null, null, null);
        }
    }
}