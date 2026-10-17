package com.llamaexpressbackend.admin.dto;

import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.order.ServiceType;
import com.llamaexpressbackend.order.Tariff;
import java.math.BigDecimal;
import java.time.Instant;

public record TariffResponse(Long id, ServiceType serviceType, VehicleType vehicleType, BigDecimal baseFare,
        BigDecimal perKmFare, BigDecimal minFare, Instant activeFrom, Instant activeTo,
        boolean active) {
    public static TariffResponse from(Tariff t, Instant now) {
        return new TariffResponse(t.getId(), t.getServiceType(), t.getVehicleType(), t.getBaseFare(),
                t.getPerKmFare(), t.getMinFare(), t.getActiveFrom(), t.getActiveTo(), t.isActiveAt(now));
    }
}