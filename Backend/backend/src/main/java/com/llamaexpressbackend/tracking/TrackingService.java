package com.llamaexpressbackend.tracking;

import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.courier.CourierLocationStore;
import com.llamaexpressbackend.courier.CourierProfileRepository;
import com.llamaexpressbackend.order.*;
import com.llamaexpressbackend.order.dto.LocationResponse;
import com.llamaexpressbackend.order.dto.TimelineEntry;
import com.llamaexpressbackend.tracking.dto.CourierInfo;
import com.llamaexpressbackend.tracking.dto.LocationPoint;
import com.llamaexpressbackend.tracking.dto.TrackingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingService {

    private final DeliveryOrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CourierProfileRepository profileRepository;
    private final CourierLocationRepository locationRepository;
    private final CourierLocationStore locationStore;

    public TrackingResponse forCustomer(Long customerId, String code) {
        DeliveryOrder order = orderRepository.findByOrderCodeAndCustomer_Id(code, customerId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order tidak ditemukan"));
        return build(order, true);
    }

    public TrackingResponse forPublic(String trackingToken) {
        DeliveryOrder order = orderRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new NotFoundException("TRACKING_NOT_FOUND", "Link lacak tidak valid"));
        return build(order, false);
    }

    private TrackingResponse build(DeliveryOrder order, boolean includeContact) {
        CourierInfo courier = null;
        LocationPoint lastLocation = null;
        List<LocationPoint> route = List.of();

        if (order.getCourier() != null) {
            Long courierId = order.getCourier().getId();
            courier = profileRepository.findWithUserByUserId(courierId)
                    .map(p -> CourierInfo.from(p, includeContact)).orElse(null);
            if (OrderStatus.COURIER_ACTIVE.contains(order.getStatus())) {
                lastLocation = locationStore.lastPosition(courierId)
                        .map(p -> new LocationPoint(p.lat(), p.lng(), p.at())).orElse(null);
            }
            route = locationRepository.findTop500ByOrderIdOrderByRecordedAtAsc(order.getId()).stream()
                    .map(l -> new LocationPoint(l.getLat().doubleValue(), l.getLng().doubleValue(), l.getRecordedAt()))
                    .toList();
        }

        List<TimelineEntry> timeline = historyRepository.findByOrder_IdOrderByIdAsc(order.getId()).stream()
                .map(TimelineEntry::from).toList();

        return new TrackingResponse(order.getOrderCode(), order.getStatus(),
                LocationResponse.from(order.getPickup()), LocationResponse.from(order.getDropoff()),
                courier, lastLocation, route, timeline);
    }
}