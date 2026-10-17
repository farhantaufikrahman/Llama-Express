package com.llamaexpressbackend.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.midtrans")
public record MidtransProperties(String provider, String serverKey, String clientKey,
        String snapBaseUrl, String apiBaseUrl,
        int expiryMinutes, String notificationUrl) {
}