package com.llamaexpressbackend.courier.dto;

import jakarta.validation.constraints.*;

public record LocationUpdateRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng) {
}
