package com.llamaexpressbackend.order.dto;

import com.llamaexpressbackend.order.*;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(String code, OrderStatus status, ServiceType serviceType,
        String dropoffAddress, BigDecimal priceTotal, Instant createdAt) {
    public static OrderSummaryResponse from(DeliveryOrder o) {
        return new OrderSummaryResponse(o.getOrderCode(), o.getStatus(), o.getServiceType(),
                o.getDropoff().getAddress(), o.getPriceTotal(), o.getCreatedAt());
    }
}