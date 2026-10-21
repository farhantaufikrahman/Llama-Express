package com.llamaexpressbackend.order.dto;

import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.order.ServiceType;
import java.math.BigDecimal;

public record QuoteResponse(ServiceType serviceType, VehicleType vehicleType, int distanceMeters, int etaMinutes,
        BigDecimal baseFare, BigDecimal distanceFare, BigDecimal total) {
}
