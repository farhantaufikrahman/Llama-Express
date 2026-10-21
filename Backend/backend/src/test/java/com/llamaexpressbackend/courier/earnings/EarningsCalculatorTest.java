package com.llamaexpressbackend.courier.earnings;

import com.llamaexpressbackend.courier.earnings.dto.EarningsSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static com.llamaexpressbackend.order.PaymentMethod.CASH;
import static com.llamaexpressbackend.order.PaymentMethod.MIDTRANS;
import static org.assertj.core.api.Assertions.assertThat;

class EarningsCalculatorTest {

    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private static final BigDecimal SHARE = new BigDecimal("0.80");

    @Test
    void groupsByJakartaDate_notUtcDate() {
        List<EarningRow> rows = List.of(
                row("2026-10-16T03:00:00Z", 12000, CASH), // 16 Okt 10:00 WIB
                row("2026-10-16T17:30:00Z", 20000, MIDTRANS), // 17 Okt 00:30 WIB ← tanggal UTC-nya masih 16!
                row("2026-10-17T05:00:00Z", 8000, CASH)); // 17 Okt 12:00 WIB

        EarningsSummary s = EarningsCalculator.summarize(rows,
                LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-17"), WIB, SHARE);

        assertThat(s.days()).hasSize(2);
        assertThat(s.days().get(0).deliveredCount()).isEqualTo(1); // 16 Okt
        assertThat(s.days().get(0).courierEarning()).isEqualByComparingTo("9600");
        assertThat(s.days().get(1).deliveredCount()).isEqualTo(2); // 17 Okt
        assertThat(s.days().get(1).courierEarning()).isEqualByComparingTo("22400");
    }

    @Test
    void computesTotals_cashCollected_andNetPayout() {
        List<EarningRow> rows = List.of(
                row("2026-10-16T03:00:00Z", 12000, CASH),
                row("2026-10-16T17:30:00Z", 20000, MIDTRANS),
                row("2026-10-17T05:00:00Z", 8000, CASH));

        EarningsSummary s = EarningsCalculator.summarize(rows,
                LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-17"), WIB, SHARE);

        assertThat(s.deliveredCount()).isEqualTo(3);
        assertThat(s.grossTotal()).isEqualByComparingTo("40000");
        assertThat(s.courierEarning()).isEqualByComparingTo("32000"); // 80%
        assertThat(s.cashCollected()).isEqualByComparingTo("20000"); // 12000 + 8000 tunai
        assertThat(s.netPayout()).isEqualByComparingTo("12000"); // 32000 − 20000
    }

    @Test
    void emptyDays_areStillListed() {
        EarningsSummary s = EarningsCalculator.summarize(List.of(),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-07"), WIB, SHARE);

        assertThat(s.days()).hasSize(7);
        assertThat(s.grossTotal()).isEqualByComparingTo("0");
    }

    private static EarningRow row(String deliveredAt, long price, com.llamaexpressbackend.order.PaymentMethod method) {
        return new EarningRow(Instant.parse(deliveredAt), BigDecimal.valueOf(price), method);
    }
}