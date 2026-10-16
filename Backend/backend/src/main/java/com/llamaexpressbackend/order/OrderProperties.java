package com.llamaexpressbackend.order;

import com.llamaexpressbackend.courier.VehicleType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "app.order")
public record OrderProperties(
        @Positive double roadFactor,
        @Positive int maxDistanceKm,
        @NotEmpty Map<VehicleType, Integer> avgSpeedKmh,
        @NotEmpty Map<VehicleType, Integer> maxWeightKg) {
}