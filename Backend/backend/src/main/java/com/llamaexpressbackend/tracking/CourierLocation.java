package com.llamaexpressbackend.tracking;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "courier_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourierLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(precision = 6, scale = 2)
    private BigDecimal speed;

    @Column(precision = 5, scale = 2)
    private BigDecimal heading;

    @Column(nullable = false)
    private Instant recordedAt;

    public static CourierLocation from(CourierLocationEvent e, Long orderId) {
        CourierLocation l = new CourierLocation();
        l.courierId = e.courierId();
        l.orderId = orderId;
        l.lat = BigDecimal.valueOf(e.lat()).setScale(6, RoundingMode.HALF_UP);
        l.lng = BigDecimal.valueOf(e.lng()).setScale(6, RoundingMode.HALF_UP);
        l.speed = (e.speed() == null) ? null : BigDecimal.valueOf(e.speed()).setScale(2, RoundingMode.HALF_UP);
        l.heading = (e.heading() == null) ? null : BigDecimal.valueOf(e.heading()).setScale(2, RoundingMode.HALF_UP);
        l.recordedAt = e.recordedAt();
        return l;
    }
}