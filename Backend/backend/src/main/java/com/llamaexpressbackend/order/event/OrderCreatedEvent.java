package com.llamaexpressbackend.order.event;

import com.llamaexpressbackend.common.kafka.KafkaTopics;
import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.order.DeliveryOrder;
import com.llamaexpressbackend.order.PaymentMethod;
import com.llamaexpressbackend.order.ServiceType;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        String orderCode, Long customerId, ServiceType serviceType, VehicleType vehicleType,
        BigDecimal pickupLat, BigDecimal pickupLng, BigDecimal dropoffLat, BigDecimal dropoffLng,
        PaymentMethod paymentMethod, BigDecimal priceTotal, Instant occurredAt) {

    public static final String TYPE = KafkaTopics.ORDER_CREATED;

    public static OrderCreatedEvent from(DeliveryOrder o, Instant at) {
        return new OrderCreatedEvent(o.getOrderCode(), o.getCustomer().getId(), o.getServiceType(), o.getVehicleType(),
                o.getPickup().getLat(), o.getPickup().getLng(), o.getDropoff().getLat(), o.getDropoff().getLng(),
                o.getPaymentMethod(), o.getPriceTotal(), at);
    }
}