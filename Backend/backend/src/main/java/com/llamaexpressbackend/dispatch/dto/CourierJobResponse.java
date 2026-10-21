package com.llamaexpressbackend.dispatch.dto;

import com.llamaexpressbackend.order.*;
import com.llamaexpressbackend.order.dto.LocationResponse;

import java.math.BigDecimal;
import java.time.Instant;

public record CourierJobResponse(String code, OrderStatus status, ItemCategory itemCategory, String itemDesc,
        BigDecimal weightKg, LocationResponse pickup, LocationResponse dropoff,
        String recipientName, String recipientPhone, String notes,
        BigDecimal priceTotal, PaymentMethod paymentMethod, Instant assignedAt) {

    public static CourierJobResponse from(DeliveryOrder o) {
        return new CourierJobResponse(o.getOrderCode(), o.getStatus(), o.getItemCategory(), o.getItemDesc(),
                o.getWeightKg(), LocationResponse.from(o.getPickup()), LocationResponse.from(o.getDropoff()),
                o.getRecipientName(), o.getRecipientPhone(), o.getNotes(),
                o.getPriceTotal(), o.getPaymentMethod(), o.getAssignedAt());
    }
}