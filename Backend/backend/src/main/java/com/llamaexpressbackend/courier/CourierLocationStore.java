package com.llamaexpressbackend.courier;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CourierLocationStore {

    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;
    private static final Duration POSITION_TTL = Duration.ofMinutes(10);
    private final Clock clock;

    /**
     * @return kode order aktif kalau kurir sedang mengantar; kosong kalau kurir
     *         tersedia
     */
    public Optional<String> update(Long courierId, VehicleType vehicle, double lat, double lng) {
        redis.opsForValue().set(heartbeatKey(courierId), "1", HEARTBEAT_TTL);
        redis.opsForValue().set(positionKey(courierId), lat + "," + lng + "," + clock.millis(), POSITION_TTL);

        String activeOrderCode = redis.opsForValue().get(busyKey(courierId));
        if (activeOrderCode != null) {
            return Optional.of(activeOrderCode); // sedang mengantar → jangan masuk daftar tersedia
        }
        redis.opsForGeo().add(geoKey(vehicle), new Point(lng, lat), courierId.toString());
        return Optional.empty();
    }
    // public void update(Long courierId, VehicleType vehicle, double lat, double
    // lng) {
    // redis.opsForValue().set(heartbeatKey(courierId), "1", HEARTBEAT_TTL);
    // if (Boolean.TRUE.equals(redis.hasKey(busyKey(courierId)))) {
    // return; // sedang mengantar → jangan masuk daftar "tersedia"
    // }
    // // Point(x, y) = Point(LONGITUDE, LATITUDE)
    // redis.opsForGeo().add(geoKey(vehicle), new Point(lng, lat),
    // courierId.toString());
    // }

    public void markBusy(Long courierId, VehicleType vehicle, String orderCode) {
        redis.opsForValue().set(busyKey(courierId), orderCode);
        redis.opsForGeo().remove(geoKey(vehicle), courierId.toString());
    }
    // public void markBusy(Long courierId, VehicleType vehicle) {
    // redis.opsForValue().set(busyKey(courierId), "1");
    // redis.opsForGeo().remove(geoKey(vehicle), courierId.toString());
    // }

    public void markAvailable(Long courierId) {
        redis.delete(busyKey(courierId));
    }
    // public void markAvailable(Long courierId) {
    // redis.delete(busyKey(courierId)); // masuk lagi ke daftar tersedia pada
    // kiriman lokasi berikutnya
    // }

    static String busyKey(Long courierId) {
        return "courier:busy:" + courierId;
    }

    // public void update(Long courierId, VehicleType vehicle, double lat, double
    // lng) {
    // // Point(x, y) = Point(LONGITUDE, LATITUDE) — urutan ini sering tertukar!
    // redis.opsForGeo().add(geoKey(vehicle), new Point(lng, lat),
    // courierId.toString());
    // redis.opsForValue().set(heartbeatKey(courierId), "1", HEARTBEAT_TTL);
    // }

    public void remove(Long courierId, VehicleType vehicle) {
        redis.opsForGeo().remove(geoKey(vehicle), courierId.toString());
        redis.delete(heartbeatKey(courierId));
    }

    /**
     * Kurir tersedia terdekat, urut dari yang paling dekat; kurir tanpa heartbeat
     * dibersihkan
     */
    public List<NearbyCourier> findNearby(VehicleType vehicle, double lat, double lng, double radiusKm, int limit) {
        GeoResults<GeoLocation<String>> results = redis.opsForGeo().search(
                geoKey(vehicle),
                GeoReference.fromCoordinate(lng, lat),
                new Distance(radiusKm, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit * 3L)); // ambil lebih, karena sebagian mungkin sudah basi
        if (results == null) {
            return List.of();
        }

        List<NearbyCourier> nearby = new ArrayList<>();
        for (GeoResult<GeoLocation<String>> r : results) {
            String member = r.getContent().getName();
            Long courierId = Long.valueOf(member);
            if (Boolean.TRUE.equals(redis.hasKey(heartbeatKey(courierId)))) {
                nearby.add(new NearbyCourier(courierId, r.getDistance().getValue()));
                if (nearby.size() == limit)
                    break;
            } else {
                redis.opsForGeo().remove(geoKey(vehicle), member); // heartbeat kedaluwarsa → keluarkan
            }
        }
        return nearby;
    }

    public Optional<LastPosition> lastPosition(Long courierId) {
        String value = redis.opsForValue().get(positionKey(courierId));
        if (value == null) {
            return Optional.empty();
        }
        String[] p = value.split(",");
        return Optional.of(new LastPosition(Double.parseDouble(p[0]), Double.parseDouble(p[1]),
                Instant.ofEpochMilli(Long.parseLong(p[2]))));
    }

    public record LastPosition(double lat, double lng, Instant at) {
    }

    static String positionKey(Long courierId) {
        return "courier:pos:" + courierId;
    }

    static String geoKey(VehicleType vehicle) {
        return "geo:couriers:available:" + vehicle.name();
    }

    static String heartbeatKey(Long courierId) {
        return "courier:heartbeat:" + courierId;
    }
}