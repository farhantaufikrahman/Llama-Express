package com.llamaexpressbackend.order;

import com.llamaexpressbackend.common.error.BadRequestException;
import com.llamaexpressbackend.order.dto.LocationRequest;
import com.llamaexpressbackend.order.dto.QuoteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.llamaexpressbackend.courier.VehicleType.CAR;
import static com.llamaexpressbackend.courier.VehicleType.MOTOR;
import static com.llamaexpressbackend.order.ServiceType.INSTANT;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-16T03:00:00Z");

    @Mock
    TariffRepository tariffRepository;
    PricingService pricing;

    @BeforeEach
    void setUp() {
        var props = new OrderProperties(1.3, 50, Map.of(MOTOR, 25, CAR, 20), Map.of(MOTOR, 20, CAR, 200));
        pricing = new PricingService(tariffRepository, props, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void quote_appliesRoadFactor_andRoundsUpTo500() {
        givenMotorInstantTariff();
        // selisih 0.01° lintang ≈ 1111.95 m garis lurus × 1.3 = 1446 m
        QuoteResponse q = pricing.quote(INSTANT, MOTOR, loc(-6.90, 107.60), loc(-6.91, 107.60), null);

        assertThat(q.distanceMeters()).isEqualTo(1446);
        assertThat(q.distanceFare()).isEqualByComparingTo("3615"); // 2500 × 1.446
        assertThat(q.total()).isEqualByComparingTo("12000"); // 8000 + 3615 = 11615 → dibulatkan ke 12000
        assertThat(q.etaMinutes()).isEqualTo(14); // ceil(1.446 / 25 × 60) = 4 + 10 menit buffer
    }

    @Test
    void quote_shortDistance_usesMinimumFare() {
        givenMotorInstantTariff();
        QuoteResponse q = pricing.quote(INSTANT, MOTOR, loc(-6.900, 107.60), loc(-6.901, 107.60), null);

        assertThat(q.distanceMeters()).isEqualTo(145);
        assertThat(q.total()).isEqualByComparingTo("10000"); // 8363 < min 10000
    }

    @Test
    void quote_overweightMotor_isRejected_beforeQueryingTariff() {
        assertThatThrownBy(
                () -> pricing.quote(INSTANT, MOTOR, loc(-6.90, 107.60), loc(-6.91, 107.60), BigDecimal.valueOf(25)))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", "OVERWEIGHT");
        verifyNoInteractions(tariffRepository);
    }

    @Test
    void quote_tooFar_isRejected() {
        // 0.5° ≈ 55.6 km × 1.3 = 72 km > 50 km
        assertThatThrownBy(() -> pricing.quote(INSTANT, MOTOR, loc(-6.90, 107.60), loc(-7.40, 107.60), null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", "DISTANCE_TOO_FAR");
    }

    private void givenMotorInstantTariff() {
        when(tariffRepository.findActive(INSTANT, MOTOR, NOW)).thenReturn(List.of(
                Tariff.create(INSTANT, MOTOR, bd(8000), bd(2500), bd(10000), NOW.minusSeconds(86_400))));
    }

    private static LocationRequest loc(double lat, double lng) {
        return new LocationRequest("Titik", lat, lng);
    }

    private static BigDecimal bd(long v) {
        return BigDecimal.valueOf(v);
    }
}