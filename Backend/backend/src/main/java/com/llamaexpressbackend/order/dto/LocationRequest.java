package com.llamaexpressbackend.order.dto;

import jakarta.validation.constraints.*;

public record LocationRequest(
        @NotBlank @Size(max = 255) String address,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng) {
}
