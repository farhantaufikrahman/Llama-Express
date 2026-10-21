package com.llamaexpressbackend.rating.dto;

import jakarta.validation.constraints.*;

public record RateRequest(@NotNull @Min(1) @Max(5) Integer score, @Size(max = 500) String review) {
}
