package com.llamaexpressbackend.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.llamaexpressbackend.order.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void happyPath_isAllowedStepByStep() {
        assertThat(CREATED.canTransitionTo(SEARCHING_COURIER)).isTrue();
        assertThat(SEARCHING_COURIER.canTransitionTo(ASSIGNED)).isTrue();
        assertThat(ASSIGNED.canTransitionTo(PICKING_UP)).isTrue();
        assertThat(PICKING_UP.canTransitionTo(PICKED_UP)).isTrue();
        assertThat(PICKED_UP.canTransitionTo(IN_TRANSIT)).isTrue();
        assertThat(IN_TRANSIT.canTransitionTo(DELIVERED)).isTrue();
    }

    @Test
    void cannotSkipSteps() {
        assertThat(SEARCHING_COURIER.canTransitionTo(DELIVERED)).isFalse();
        assertThat(CREATED.canTransitionTo(PICKED_UP)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = { "DELIVERED", "CANCELLED", "FAILED" })
    void terminalStates_haveNoExit(OrderStatus terminal) {
        assertThat(terminal.isTerminal()).isTrue();
        for (OrderStatus next : OrderStatus.values()) {
            assertThat(terminal.canTransitionTo(next)).isFalse();
        }
    }

    @Test
    void customerCannotCancel_afterPickup() {
        assertThat(ASSIGNED.isCancellableByCustomer()).isTrue();
        assertThat(PICKED_UP.isCancellableByCustomer()).isFalse();
        assertThat(IN_TRANSIT.isCancellableByCustomer()).isFalse();
    }
}