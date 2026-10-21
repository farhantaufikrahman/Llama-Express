package com.llamaexpressbackend.rating.dto;

import java.math.BigDecimal;

public record RatingResponse(String orderCode, int score, BigDecimal courierRatingAvg) {
}