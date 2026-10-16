package com.llamaexpressbackend.auth.dto;

import com.llamaexpressbackend.courier.VehicleType;
import jakarta.validation.constraints.*;

public record RegisterCourierRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Pattern(regexp = "^(\\+62|62|0)8[0-9]{8,12}$", message = "Format nomor HP tidak valid") String phone,
        @NotBlank @Size(min = 8, max = 72, message = "Password 8–72 karakter") String password,
        @NotNull VehicleType vehicleType,
        @NotBlank @Size(max = 15) String plateNumber,
        @Size(max = 30) String licenseNo) {
}