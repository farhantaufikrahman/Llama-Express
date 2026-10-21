package com.llamaexpressbackend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 100) String fullName,
        @Pattern(regexp = "^(\\+62|62|0)8[0-9]{8,12}$", message = "Format nomor HP tidak valid") String phone) {
}
