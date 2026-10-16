package com.llamaexpressbackend.order;

import com.llamaexpressbackend.common.error.BadRequestException;
import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.order.dto.LocationRequest;
import com.llamaexpressbackend.order.dto.QuoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class PricingService {

    private static final double EARTH_RADIUS_M = 6_371_000;
    private static final BigDecimal ROUNDING_STEP = BigDecimal.valueOf(500);
    private static final int PICKUP_BUFFER_MIN = 10;
    private static final int MIN_DISTANCE_M = 100;

    private final TariffRepository tariffRepository;
    private final OrderProperties props;
    private final Clock clock;

    public QuoteResponse quote(ServiceType service, VehicleType vehicle,
            LocationRequest pickup, LocationRequest dropoff, BigDecimal weightKg) {
        validateWeight(vehicle, weightKg);

        int distanceM = roadDistanceMeters(pickup.lat(), pickup.lng(), dropoff.lat(), dropoff.lng());
        if (distanceM < MIN_DISTANCE_M) {
            throw new BadRequestException("DISTANCE_TOO_SHORT", "Titik jemput dan antar terlalu dekat");
        }
        if (distanceM > props.maxDistanceKm() * 1000) {
            throw new BadRequestException("DISTANCE_TOO_FAR", "Jarak melebihi batas " + props.maxDistanceKm() + " km");
        }

        Tariff tariff = tariffRepository.findActive(service, vehicle, clock.instant()).stream().findFirst()
                .orElseThrow(() -> new BadRequestException("TARIFF_NOT_AVAILABLE",
                        "Layanan belum tersedia untuk kombinasi ini"));

        BigDecimal km = BigDecimal.valueOf(distanceM).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
        BigDecimal distanceFare = tariff.getPerKmFare().multiply(km).setScale(0, RoundingMode.HALF_UP);
        BigDecimal raw = tariff.getBaseFare().add(distanceFare).max(tariff.getMinFare());
        BigDecimal total = raw.divide(ROUNDING_STEP, 0, RoundingMode.CEILING).multiply(ROUNDING_STEP); // bulatkan ke
                                                                                                       // atas per Rp500

        return new QuoteResponse(service, vehicle, distanceM, etaMinutes(distanceM, vehicle),
                tariff.getBaseFare().setScale(0, RoundingMode.HALF_UP), distanceFare, total);
    }

    int roadDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        return (int) Math.round(haversineMeters(lat1, lng1, lat2, lng2) * props.roadFactor());
    }

    /** Jarak garis lurus di permukaan bumi (great-circle) */
    static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
    }

    private int etaMinutes(int distanceM, VehicleType vehicle) {
        double hours = distanceM / 1000.0 / props.avgSpeedKmh().get(vehicle);
        return (int) Math.ceil(hours * 60) + PICKUP_BUFFER_MIN;
    }

    private void validateWeight(VehicleType vehicle, BigDecimal weightKg) {
        int max = props.maxWeightKg().get(vehicle);
        if (weightKg != null && weightKg.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new BadRequestException("OVERWEIGHT", "Berat maksimal untuk " + vehicle + " adalah " + max + " kg");
        }
    }
}