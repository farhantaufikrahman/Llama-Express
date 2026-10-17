package com.llamaexpressbackend.admin.dto;

import com.llamaexpressbackend.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record AdminDashboardResponse(LocalDate date, long ordersToday, long activeOrders, long searchingCourier,
        long couriersOnline, long deliveredToday, BigDecimal revenueToday,
        Double avgDeliveryMinutes, Map<OrderStatus, Long> statusBreakdown) {
}
