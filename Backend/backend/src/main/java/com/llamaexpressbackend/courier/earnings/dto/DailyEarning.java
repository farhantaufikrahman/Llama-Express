package com.llamaexpressbackend.courier.earnings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyEarning(LocalDate date, int deliveredCount, BigDecimal grossTotal, BigDecimal courierEarning) {
}
