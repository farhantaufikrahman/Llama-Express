package com.llamaexpressbackend.admin.dto;

import com.llamaexpressbackend.order.OrderStatus;
import com.llamaexpressbackend.order.PaymentMethod;
import com.llamaexpressbackend.order.ServiceType;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record AdminOrderFilter(
        OrderStatus status, ServiceType serviceType, PaymentMethod paymentMethod,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        String q) {
}
