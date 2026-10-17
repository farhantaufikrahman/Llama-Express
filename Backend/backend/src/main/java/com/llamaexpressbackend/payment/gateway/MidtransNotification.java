package com.llamaexpressbackend.payment.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true) // Midtrans bisa menambah field baru kapan saja
public record MidtransNotification(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("status_code") String statusCode,
        @JsonProperty("gross_amount") String grossAmount,
        @JsonProperty("signature_key") String signatureKey,
        @JsonProperty("transaction_status") String transactionStatus,
        @JsonProperty("fraud_status") String fraudStatus,
        @JsonProperty("payment_type") String paymentType,
        @JsonProperty("transaction_id") String transactionId) {
}