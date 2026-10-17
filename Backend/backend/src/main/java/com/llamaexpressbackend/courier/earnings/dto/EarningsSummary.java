package com.llamaexpressbackend.courier.earnings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EarningsSummary(
        LocalDate from, LocalDate to,
        int deliveredCount,
        BigDecimal grossTotal, // total ongkir yang dibayar customer
        BigDecimal courierEarning, // bagian kurir
        BigDecimal cashCollected, // uang tunai yang SUDAH dipegang kurir (order CASH)
        BigDecimal netPayout, // yang harus ditransfer platform ke kurir (negatif = kurir menyetor ke
                              // platform)
        List<DailyEarning> days) {
}