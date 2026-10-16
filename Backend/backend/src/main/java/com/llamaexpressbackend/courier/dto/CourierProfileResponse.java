package com.llamaexpressbackend.courier.dto;

import com.llamaexpressbackend.courier.*;
import java.math.BigDecimal;

public record CourierProfileResponse(Long userId, String fullName, String phone, VehicleType vehicleType,
        String plateNumber, VerificationStatus verificationStatus,
        boolean online, BigDecimal ratingAvg, int ratingCount) {
    public static CourierProfileResponse from(CourierProfile p) {
        return new CourierProfileResponse(p.getUserId(), p.getUser().getFullName(), p.getUser().getPhone(),
                p.getVehicleType(), p.getPlateNumber(), p.getVerificationStatus(),
                p.isOnline(), p.getRatingAvg(), p.getRatingCount());
    }
}