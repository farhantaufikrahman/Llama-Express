package com.llamaexpressbackend.common.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled, Map<String, @Valid Rule> rules) {

    public RateLimitProperties {
        rules = (rules == null) ? Map.of() : rules;
    }

    public record Rule(@NotEmpty List<String> paths, @Positive int limit, @NotNull Duration window) {
    }
}