package com.llamaexpressbackend.order.dto;

import com.llamaexpressbackend.order.GeoPoint;
import java.math.BigDecimal;

public record LocationResponse(String address, BigDecimal lat, BigDecimal lng) {
    public static LocationResponse from(GeoPoint p) {
        return new LocationResponse(p.getAddress(), p.getLat(), p.getLng());
    }
}