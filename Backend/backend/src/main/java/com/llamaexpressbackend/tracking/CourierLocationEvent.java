package com.llamaexpressbackend.tracking;

import java.time.Instant;

public record CourierLocationEvent(String orderCode, Long courierId, double lat, double lng,
        Double speed, Double heading, Instant recordedAt) {
}