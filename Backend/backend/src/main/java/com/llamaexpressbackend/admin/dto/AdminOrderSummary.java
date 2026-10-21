package com.llamaexpressbackend.admin.dto;

import com.llamaexpressbackend.order.*;
import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderSummary(String code, OrderStatus status, ServiceType serviceType, String customerName,
        String courierName, BigDecimal priceTotal, PaymentMethod paymentMethod,
        PaymentStatus paymentStatus, Instant createdAt) {
    public static AdminOrderSummary from(DeliveryOrder o) {
        return new AdminOrderSummary(o.getOrderCode(), o.getStatus(), o.getServiceType(),
                o.getCustomer().getFullName(), o.getCourier() == null ? null : o.getCourier().getFullName(),
                o.getPriceTotal(), o.getPaymentMethod(), o.getPaymentStatus(), o.getCreatedAt());
    }
}
