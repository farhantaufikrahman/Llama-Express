package com.llamaexpressbackend.admin.dto;

import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.order.ServiceType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TariffRequest(
        @NotNull ServiceType serviceType,
        @NotNull VehicleType vehicleType,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 0) BigDecimal baseFare,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 0) BigDecimal perKmFare,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 0) BigDecimal minFare,
        OffsetDateTime activeFrom) {
} // kosong = berlaku sekarang; contoh: "2026-11-01T00:00:00+07:00"
