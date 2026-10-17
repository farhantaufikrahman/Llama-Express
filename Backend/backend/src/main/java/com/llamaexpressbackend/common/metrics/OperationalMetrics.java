package com.llamaexpressbackend.common.metrics;

import com.llamaexpressbackend.common.outbox.OutboxEventRepository;
import com.llamaexpressbackend.courier.CourierProfileRepository;
import com.llamaexpressbackend.order.DeliveryOrderRepository;
import com.llamaexpressbackend.order.OrderStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OperationalMetrics {

    public OperationalMetrics(MeterRegistry registry, OutboxEventRepository outbox,
            CourierProfileRepository couriers, DeliveryOrderRepository orders) {
        // Naik terus = Kafka bermasalah atau OutboxPublisher macet → paling penting
        // untuk alert
        Gauge.builder("llama.outbox.pending", outbox, OutboxEventRepository::countByPublishedAtIsNull)
                .description("Event outbox yang belum terkirim ke Kafka")
                .register(registry);
        Gauge.builder("llama.couriers.online", couriers, CourierProfileRepository::countByOnlineTrue)
                .description("Kurir yang sedang online")
                .register(registry);
        Gauge.builder("llama.orders.searching", orders, r -> r.countByStatus(OrderStatus.SEARCHING_COURIER))
                .description("Order yang sedang menunggu kurir")
                .register(registry);
    }
}