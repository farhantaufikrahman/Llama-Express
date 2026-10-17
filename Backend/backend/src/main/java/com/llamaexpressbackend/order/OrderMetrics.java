package com.llamaexpressbackend.order;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;

@Component
public class OrderMetrics {

    private final MeterRegistry registry;
    private final Timer deliveryDuration;

    public OrderMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.deliveryDuration = Timer.builder("llama.orders.delivery.duration")
                .description("Waktu dari order dibuat sampai paket diterima")
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMinutes(1))
                .maximumExpectedValue(Duration.ofHours(12))
                .register(registry);
    }

    public void created(DeliveryOrder order) {
        String service = order.getServiceType().name();
        String vehicle = order.getVehicleType().name();
        String payment = order.getPaymentMethod().name();
        afterCommit(() -> registry.counter("llama.orders.created",
                "service_type", service, "vehicle_type", vehicle, "payment_method", payment).increment());
    }

    public void transitioned(DeliveryOrder order) {
        OrderStatus status = order.getStatus();
        Instant createdAt = order.getCreatedAt();
        Instant deliveredAt = order.getDeliveredAt();
        afterCommit(() -> {
            registry.counter("llama.orders.transitions", "status", status.name()).increment();
            if (status == OrderStatus.DELIVERED && createdAt != null && deliveredAt != null) {
                deliveryDuration.record(Duration.between(createdAt, deliveredAt));
            }
        });
    }

    /**
     * Hitung hanya kalau transaksi COMMIT — order yang di-rollback tidak boleh ikut
     * terhitung
     */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}