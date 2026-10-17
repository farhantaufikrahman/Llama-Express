package com.llamaexpressbackend.tracking.dto;

import com.llamaexpressbackend.order.OrderStatus;
import com.llamaexpressbackend.order.dto.LocationResponse;
import com.llamaexpressbackend.order.dto.TimelineEntry;
import java.util.List;

public record TrackingResponse(String orderCode, OrderStatus status, LocationResponse pickup, LocationResponse dropoff,
        CourierInfo courier, LocationPoint lastLocation,
        List<LocationPoint> route, List<TimelineEntry> timeline) {
}