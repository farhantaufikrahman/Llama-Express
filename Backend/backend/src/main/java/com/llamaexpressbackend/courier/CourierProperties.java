package com.llamaexpressbackend.courier;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.ZoneId;

@Validated
@ConfigurationProperties(prefix = "app.courier")
public record CourierProperties(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal earningShare,
        @NotNull ZoneId businessZone) {
}