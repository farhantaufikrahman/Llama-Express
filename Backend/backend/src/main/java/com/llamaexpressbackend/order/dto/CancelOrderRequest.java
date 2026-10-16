package com.llamaexpressbackend.order.dto;

import jakarta.validation.constraints.*;

public record CancelOrderRequest(@NotBlank @Size(max = 255) String reason) {
}
