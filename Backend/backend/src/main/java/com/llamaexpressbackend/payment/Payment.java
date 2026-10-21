package com.llamaexpressbackend.payment;

import com.llamaexpressbackend.common.persistance.BaseEntity;
import com.llamaexpressbackend.order.DeliveryOrder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DeliveryOrder order;

    @Column(nullable = false, length = 20)
    private String provider = "MIDTRANS";

    /**
     * order_id di sisi Midtrans — unik per PERCOBAAN bayar (LX-...-P1, -P2, ...)
     */
    @Column(nullable = false, length = 50, unique = true)
    private String merchantOrderId;

    @Column(length = 64)
    private String transactionId;

    @Column(length = 30)
    private String paymentType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    @Column(length = 100)
    private String snapToken;

    @Column(length = 500)
    private String redirectUrl;

    @Lob
    private String rawNotification;

    private Instant paidAt;

    private Instant expiresAt;

    public static Payment pending(DeliveryOrder order, String merchantOrderId, BigDecimal amount,
            String snapToken, String redirectUrl, Instant expiresAt) {
        Payment p = new Payment();
        p.order = order;
        p.merchantOrderId = merchantOrderId;
        p.amount = amount;
        p.snapToken = snapToken;
        p.redirectUrl = redirectUrl;
        p.expiresAt = expiresAt;
        return p;
    }

    /** Token masih bisa dipakai kalau PENDING dan belum mendekati kedaluwarsa */
    public boolean isReusable(Instant now) {
        return status == PaymentTransactionStatus.PENDING && snapToken != null
                && expiresAt != null && now.isBefore(expiresAt.minusSeconds(60));
    }

    public void apply(PaymentTransactionStatus newStatus, String transactionId, String paymentType,
            String rawNotification, Instant now) {
        this.status = newStatus;
        if (transactionId != null)
            this.transactionId = transactionId;
        if (paymentType != null)
            this.paymentType = paymentType;
        this.rawNotification = rawNotification;
        if (newStatus == PaymentTransactionStatus.PAID && paidAt == null) {
            this.paidAt = now;
        }
    }
}