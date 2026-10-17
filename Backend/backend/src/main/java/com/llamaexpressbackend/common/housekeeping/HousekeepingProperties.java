package com.llamaexpressbackend.common.housekeeping;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.housekeeping")
public record HousekeepingProperties(
        @NotNull Duration outboxRetention,
        @NotNull Duration processedEventsRetention,
        @Positive int batchSize) {
}