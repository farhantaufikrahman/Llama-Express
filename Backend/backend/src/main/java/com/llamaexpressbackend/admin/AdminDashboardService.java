package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.AdminDashboardResponse;
import com.llamaexpressbackend.admin.dto.DeliveredRow;
import com.llamaexpressbackend.admin.dto.StatusCount;
import com.llamaexpressbackend.courier.CourierProfileRepository;
import com.llamaexpressbackend.courier.CourierProperties;
import com.llamaexpressbackend.order.DeliveryOrderRepository;
import com.llamaexpressbackend.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final DeliveryOrderRepository orderRepository;
    private final CourierProfileRepository profileRepository;
    private final CourierProperties courierProperties; // pakai businessZone yang sama (Asia/Jakarta)
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminDashboardResponse summary() {
        ZoneId zone = courierProperties.businessZone();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant startOfDay = today.atStartOfDay(zone).toInstant();

        Map<OrderStatus, Long> breakdown = new EnumMap<>(OrderStatus.class);
        for (StatusCount c : orderRepository.countByStatusSince(startOfDay)) {
            breakdown.put(c.status(), c.total());
        }

        List<DeliveredRow> delivered = orderRepository.findDeliveredSince(OrderStatus.DELIVERED, startOfDay);
        BigDecimal revenue = delivered.stream().map(DeliveredRow::priceTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Double avgMinutes = delivered.isEmpty() ? null
                : delivered.stream()
                        .mapToLong(r -> Duration.between(r.createdAt(), r.deliveredAt()).toMinutes())
                        .average().orElse(0);

        return new AdminDashboardResponse(
                today,
                orderRepository.countByCreatedAtGreaterThanEqual(startOfDay),
                orderRepository.countByStatusIn(OrderStatus.COURIER_ACTIVE),
                orderRepository.countByStatus(OrderStatus.SEARCHING_COURIER),
                profileRepository.countByOnlineTrue(),
                delivered.size(),
                revenue,
                avgMinutes,
                breakdown);
    }
}