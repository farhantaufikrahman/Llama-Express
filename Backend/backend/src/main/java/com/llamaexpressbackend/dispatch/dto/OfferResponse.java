package com.llamaexpressbackend.dispatch.dto;

import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.dispatch.OrderOffer;
import com.llamaexpressbackend.order.*;
import com.llamaexpressbackend.order.dto.LocationResponse;

import java.math.BigDecimal;
import java.time.Instant;

public record OfferResponse(Long offerId, String orderCode, ServiceType serviceType, VehicleType vehicleType,
        ItemCategory itemCategory, LocationResponse pickup, LocationResponse dropoff,
        int distanceMeters, BigDecimal priceTotal, PaymentMethod paymentMethod, Instant expiresAt) {

    public static OfferResponse from(OrderOffer offer) {
        DeliveryOrder o = offer.getOrder();
        return new OfferResponse(offer.getId(), o.getOrderCode(), o.getServiceType(), o.getVehicleType(),
                o.getItemCategory(), LocationResponse.from(o.getPickup()), LocationResponse.from(o.getDropoff()),
                o.getDistanceMeters(), o.getPriceTotal(), o.getPaymentMethod(), offer.getExpiresAt());
    }
}