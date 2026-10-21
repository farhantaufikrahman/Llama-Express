package com.llamaexpressbackend.order.dto;

import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.order.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String code, OrderStatus status, ServiceType serviceType, VehicleType vehicleType,
        ItemCategory itemCategory, String itemDesc, BigDecimal weightKg,
        LocationResponse pickup, LocationResponse dropoff,
        String recipientName, String recipientPhone, String notes,
        int distanceMeters, BigDecimal priceTotal, PaymentMethod paymentMethod, PaymentStatus paymentStatus,
        String verificationCode, // HANYA untuk customer — diberikan ke penerima, kurir harus memintanya saat
                                 // serah terima
        String trackingToken, // untuk link lacak publik (langkah berikutnya)
        Instant createdAt, Instant cancelledAt, String cancelReason,
        List<TimelineEntry> timeline) {

    public static OrderResponse from(DeliveryOrder o, List<OrderStatusHistory> history) {
        return new OrderResponse(
                o.getOrderCode(), o.getStatus(), o.getServiceType(), o.getVehicleType(),
                o.getItemCategory(), o.getItemDesc(), o.getWeightKg(),
                LocationResponse.from(o.getPickup()), LocationResponse.from(o.getDropoff()),
                o.getRecipientName(), o.getRecipientPhone(), o.getNotes(),
                o.getDistanceMeters(), o.getPriceTotal(), o.getPaymentMethod(), o.getPaymentStatus(),
                o.getVerificationCode(), o.getTrackingToken(),
                o.getCreatedAt(), o.getCancelledAt(), o.getCancelReason(),
                history.stream().map(TimelineEntry::from).toList());
    }
}