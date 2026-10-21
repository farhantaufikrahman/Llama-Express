package com.llamaexpressbackend.payment.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SnapRequest(
        @JsonProperty("transaction_details") TransactionDetails transactionDetails,
        @JsonProperty("item_details") List<ItemDetail> itemDetails,
        @JsonProperty("customer_details") CustomerDetails customerDetails,
        Expiry expiry) {

    public record TransactionDetails(@JsonProperty("order_id") String orderId,
            @JsonProperty("gross_amount") long grossAmount) {
    }

    public record ItemDetail(String id, long price, int quantity, String name) {
    }

    public record CustomerDetails(@JsonProperty("first_name") String firstName, String email, String phone) {
    }

    public record Expiry(String unit, int duration) {
    }
}