package com.llamaexpressbackend.courier.earnings;

import com.llamaexpressbackend.common.error.BadRequestException;
import com.llamaexpressbackend.courier.CourierProperties;
import com.llamaexpressbackend.courier.earnings.dto.EarningsSummary;
import com.llamaexpressbackend.order.DeliveryOrderRepository;
import com.llamaexpressbackend.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class EarningsService {

    private static final int MAX_RANGE_DAYS = 62;

    private final DeliveryOrderRepository orderRepository;
    private final CourierProperties props;
    private final Clock clock;

    @Transactional(readOnly = true)
    public EarningsSummary summary(Long courierId, LocalDate from, LocalDate to) {
        ZoneId zone = props.businessZone();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate start = (from != null) ? from : today.withDayOfMonth(1); // default: awal bulan ini
        LocalDate end = (to != null) ? to : today;

        if (end.isBefore(start)) {
            throw new BadRequestException("INVALID_RANGE", "Tanggal akhir tidak boleh sebelum tanggal awal");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            throw new BadRequestException("RANGE_TOO_LONG", "Rentang maksimal " + MAX_RANGE_DAYS + " hari");
        }

        // 00:00 WIB tanggal awal s/d 00:00 WIB SETELAH tanggal akhir, diubah ke Instant
        // (UTC) untuk query
        var rows = orderRepository.findEarningRows(courierId, OrderStatus.DELIVERED,
                start.atStartOfDay(zone).toInstant(), end.plusDays(1).atStartOfDay(zone).toInstant());
        return EarningsCalculator.summarize(rows, start, end, zone, props.earningShare());
    }
}