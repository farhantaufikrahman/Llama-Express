package com.llamaexpressbackend.order.event;

import com.llamaexpressbackend.common.kafka.KafkaTopics;
import com.llamaexpressbackend.order.OrderStatus;

import java.time.Instant;

public record OrderStatusChangedEvent(String orderCode, OrderStatus fromStatus, OrderStatus toStatus,
        Long actorId, String note, Instant occurredAt) {
    public static final String TYPE = KafkaTopics.ORDER_STATUS_CHANGED;
}