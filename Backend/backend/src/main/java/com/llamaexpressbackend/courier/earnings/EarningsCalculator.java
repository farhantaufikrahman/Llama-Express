package com.llamaexpressbackend.courier.earnings;

import com.llamaexpressbackend.courier.earnings.dto.DailyEarning;
import com.llamaexpressbackend.courier.earnings.dto.EarningsSummary;
import com.llamaexpressbackend.order.PaymentMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Fungsi murni — tanpa DB, tanpa Spring → mudah diuji */
public final class EarningsCalculator {

    private EarningsCalculator() {
    }

    public static EarningsSummary summarize(List<EarningRow> rows, LocalDate from, LocalDate to,
            ZoneId zone, BigDecimal earningShare) {
        // Kelompokkan menurut tanggal LOKAL (WIB), bukan tanggal UTC
        Map<LocalDate, List<EarningRow>> byDay = rows.stream()
                .collect(Collectors.groupingBy(r -> LocalDate.ofInstant(r.deliveredAt(), zone)));

        // Semua hari dalam rentang ikut ditampilkan, termasuk yang nol — grafik
        // frontend jadi tidak bolong
        List<DailyEarning> days = from.datesUntil(to.plusDays(1))
                .map(date -> {
                    List<EarningRow> dayRows = byDay.getOrDefault(date, List.of());
                    BigDecimal gross = sum(dayRows);
                    return new DailyEarning(date, dayRows.size(), gross, share(gross, earningShare));
                })
                .toList();

        BigDecimal gross = sum(rows);
        BigDecimal earning = days.stream().map(DailyEarning::courierEarning).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cash = sum(rows.stream().filter(r -> r.paymentMethod() == PaymentMethod.CASH).toList());

        return new EarningsSummary(from, to, rows.size(), gross, earning, cash, earning.subtract(cash), days);
    }

    private static BigDecimal sum(List<EarningRow> rows) {
        return rows.stream().map(EarningRow::priceTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal share(BigDecimal gross, BigDecimal earningShare) {
        return gross.multiply(earningShare).setScale(0, RoundingMode.HALF_UP);
    }
}