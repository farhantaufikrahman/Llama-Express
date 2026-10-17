package com.llamaexpressbackend.tracking.dto;

import java.time.Instant;

public record LocationPoint(double lat, double lng, Instant at) {
}
