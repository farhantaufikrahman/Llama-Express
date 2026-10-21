package com.llamaexpressbackend.order.dto;

import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.order.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record QuoteRequest(
        @NotNull ServiceType serviceType,
        @NotNull VehicleType vehicleType,
        @NotNull @Valid LocationRequest pickup,
        @NotNull @Valid LocationRequest dropoff,
        @DecimalMin("0.1") @Digits(integer = 4, fraction = 2) BigDecimal weightKg) {
}
