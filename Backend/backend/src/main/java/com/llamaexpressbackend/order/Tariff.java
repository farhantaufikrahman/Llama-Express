package com.llamaexpressbackend.order;

import com.llamaexpressbackend.common.persistance.BaseEntity;
import com.llamaexpressbackend.courier.VehicleType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tariffs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tariff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VehicleType vehicleType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal perKmFare;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minFare;

    @Column(nullable = false)
    private Instant activeFrom;

    private Instant activeTo;

    public static Tariff create(ServiceType serviceType, VehicleType vehicleType,
            BigDecimal baseFare, BigDecimal perKmFare, BigDecimal minFare, Instant activeFrom) {
        Tariff t = new Tariff();
        t.serviceType = serviceType;
        t.vehicleType = vehicleType;
        t.baseFare = baseFare;
        t.perKmFare = perKmFare;
        t.minFare = minFare;
        t.activeFrom = activeFrom;
        return t;
    }

    public void closeAt(Instant at) {
        this.activeTo = at;
    }

    public boolean isActiveAt(Instant at) {
        return !activeFrom.isAfter(at) && (activeTo == null || activeTo.isAfter(at));
    }
}