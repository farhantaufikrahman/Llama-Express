package com.llamaexpressbackend.dispatch;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.dispatch")
public record DispatchProperties(
        @Positive double radiusKm,
        @Positive int candidatesPerRound,
        @Positive int offerTtlSeconds,
        @Positive int maxAttempts) {
}