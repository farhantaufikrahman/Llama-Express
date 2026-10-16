package com.llamaexpressbackend.courier;

import com.llamaexpressbackend.common.persistance.BaseEntity;
import com.llamaexpressbackend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "courier_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourierProfile extends BaseEntity {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VehicleType vehicleType;

    @Column(nullable = false, length = 15, unique = true)
    private String plateNumber;

    @Column(length = 30)
    private String licenseNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(nullable = false)
    private int ratingCount;

    public static String normalizePlate(String plate) {
        return plate.replace(" ", "").toUpperCase(java.util.Locale.ROOT);
    }

    public static CourierProfile create(User user, VehicleType vehicleType, String plateNumber, String licenseNo) {
        CourierProfile p = new CourierProfile();
        p.user = user;
        p.vehicleType = vehicleType;
        p.plateNumber = normalizePlate(plateNumber);
        p.licenseNo = licenseNo;
        return p;
    }

    public void verify() {
        this.verificationStatus = VerificationStatus.VERIFIED;
    }

    public void reject() {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.online = false;
    }

    public void goOnline() {
        if (verificationStatus != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Kurir belum terverifikasi");
        }
        this.online = true;
    }

    public void goOffline() {
        this.online = false;
    }

    /** Rata-rata berjalan: avg baru = (avg * n + skor) / (n + 1) */
    public void addRating(int score) {
        if (score < 1 || score > 5)
            throw new IllegalArgumentException("Skor harus 1–5");
        BigDecimal total = ratingAvg.multiply(BigDecimal.valueOf(ratingCount)).add(BigDecimal.valueOf(score));
        ratingCount++;
        ratingAvg = total.divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
    }
}
