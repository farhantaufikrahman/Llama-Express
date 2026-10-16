package com.llamaexpressbackend.courier.dto;

import jakarta.validation.constraints.NotNull;

public record CourierStatusRequest(@NotNull Boolean online) {
}
