package com.llamaexpressbackend.order.dto;

import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.order.ItemCategory;
import com.llamaexpressbackend.order.PaymentMethod;
import com.llamaexpressbackend.order.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull ServiceType serviceType,
        @NotNull VehicleType vehicleType,
        @NotNull ItemCategory itemCategory,
        @Size(max = 255) String itemDesc,
        @DecimalMin("0.1") @Digits(integer = 4, fraction = 2) BigDecimal weightKg,
        @NotNull @Valid LocationRequest pickup,
        @NotNull @Valid LocationRequest dropoff,
        @NotBlank @Size(max = 100) String recipientName,
        @NotBlank @Pattern(regexp = "^(\\+62|62|0)8[0-9]{8,12}$", message = "Format nomor HP tidak valid") String recipientPhone,
        @Size(max = 500) String notes,
        @NotNull PaymentMethod paymentMethod) {
}
