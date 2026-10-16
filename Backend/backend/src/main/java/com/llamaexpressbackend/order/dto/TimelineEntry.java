package com.llamaexpressbackend.order.dto;

import com.llamaexpressbackend.order.OrderStatus;
import com.llamaexpressbackend.order.OrderStatusHistory;
import java.time.Instant;

public record TimelineEntry(OrderStatus status, Instant at, String note) {
    public static TimelineEntry from(OrderStatusHistory h) {
        return new TimelineEntry(h.getToStatus(), h.getOccurredAt(), h.getNote());
    }
}