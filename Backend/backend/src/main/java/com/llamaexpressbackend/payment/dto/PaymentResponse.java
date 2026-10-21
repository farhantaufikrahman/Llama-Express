package com.llamaexpressbackend.payment.dto;

import com.llamaexpressbackend.payment.Payment;
import com.llamaexpressbackend.payment.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(String orderCode, String merchantOrderId, PaymentTransactionStatus status,
        BigDecimal amount, String snapToken, String redirectUrl,
        Instant expiresAt, Instant paidAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getOrder().getOrderCode(), p.getMerchantOrderId(), p.getStatus(),
                p.getAmount(), p.getSnapToken(), p.getRedirectUrl(), p.getExpiresAt(), p.getPaidAt());
    }
}