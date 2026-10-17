package com.llamaexpressbackend.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DeliveredRow(Instant createdAt, Instant deliveredAt, BigDecimal priceTotal) {
}
