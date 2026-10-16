package com.llamaexpressbackend.courier;

import com.llamaexpressbackend.common.error.ForbiddenException;
import com.llamaexpressbackend.user.Role;
import com.llamaexpressbackend.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CourierProfileTest {

    @Test
    void plateNumber_isNormalized() {
        assertThat(newCourier().getPlateNumber()).isEqualTo("D1234ABC");
    }

    @Test
    void cannotGoOnline_untilVerified() {
        CourierProfile p = newCourier();
        assertThatThrownBy(p::goOnline).isInstanceOf(ForbiddenException.class);

        p.verify();
        p.goOnline();
        assertThat(p.isOnline()).isTrue();
    }

    @Test
    void reject_forcesOffline() {
        CourierProfile p = newCourier();
        p.verify();
        p.goOnline();

        p.reject();

        assertThat(p.isOnline()).isFalse();
        assertThat(p.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    void addRating_keepsRunningAverage() {
        CourierProfile p = newCourier();
        p.addRating(5);
        p.addRating(4);
        assertThat(p.getRatingAvg()).isEqualByComparingTo("4.50");
        assertThat(p.getRatingCount()).isEqualTo(2);

        p.addRating(3); // (4.50 × 2 + 3) / 3 = 4.00
        assertThat(p.getRatingAvg()).isEqualByComparingTo("4.00");
    }

    @Test
    void addRating_rejectsOutOfRange() {
        CourierProfile p = newCourier();
        assertThatThrownBy(() -> p.addRating(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.addRating(6)).isInstanceOf(IllegalArgumentException.class);
    }

    private static CourierProfile newCourier() {
        User user = User.create(Role.COURIER, "Kurir", "kurir@test.com", "081200000000", "{bcrypt}x");
        return CourierProfile.create(user, VehicleType.MOTOR, "d 1234 abc", null);
    }
}