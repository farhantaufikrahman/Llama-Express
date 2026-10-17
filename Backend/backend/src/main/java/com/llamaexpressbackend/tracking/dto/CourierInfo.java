package com.llamaexpressbackend.tracking.dto;

import com.llamaexpressbackend.courier.CourierProfile;
import com.llamaexpressbackend.courier.VehicleType;
import java.math.BigDecimal;

public record CourierInfo(String name, String phone, VehicleType vehicleType, String plateNumber,
        BigDecimal ratingAvg) {
    /** includeContact=false (link publik) → hanya nama depan, tanpa nomor HP */
    public static CourierInfo from(CourierProfile p, boolean includeContact) {
        String fullName = p.getUser().getFullName();
        return new CourierInfo(includeContact ? fullName : fullName.split(" ")[0],
                includeContact ? p.getUser().getPhone() : null,
                p.getVehicleType(), p.getPlateNumber(), p.getRatingAvg());
    }
}