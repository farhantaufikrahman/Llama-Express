package com.llamaexpressbackend.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    CREATED, SEARCHING_COURIER, ASSIGNED, PICKING_UP, PICKED_UP, IN_TRANSIT, DELIVERED, CANCELLED, FAILED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(CREATED, EnumSet.of(SEARCHING_COURIER, CANCELLED));
        TRANSITIONS.put(SEARCHING_COURIER, EnumSet.of(ASSIGNED, CANCELLED, FAILED));
        TRANSITIONS.put(ASSIGNED, EnumSet.of(PICKING_UP, CANCELLED));
        TRANSITIONS.put(PICKING_UP, EnumSet.of(PICKED_UP, CANCELLED, FAILED));
        TRANSITIONS.put(PICKED_UP, EnumSet.of(IN_TRANSIT));
        TRANSITIONS.put(IN_TRANSIT, EnumSet.of(DELIVERED, FAILED));
        TRANSITIONS.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(FAILED, EnumSet.noneOf(OrderStatus.class));
    }
    public static final Set<OrderStatus> COURIER_ACTIVE = EnumSet.of(ASSIGNED, PICKING_UP, PICKED_UP, IN_TRANSIT);

    public boolean canTransitionTo(OrderStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }

    /** Customer hanya boleh membatalkan sebelum kurir berangkat menjemput */
    public boolean isCancellableByCustomer() {
        return this == CREATED || this == SEARCHING_COURIER || this == ASSIGNED;
    }
}