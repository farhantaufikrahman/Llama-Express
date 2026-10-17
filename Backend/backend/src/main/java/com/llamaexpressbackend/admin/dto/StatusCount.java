package com.llamaexpressbackend.admin.dto;

import com.llamaexpressbackend.order.OrderStatus;

public record StatusCount(OrderStatus status, Long total) {
}
