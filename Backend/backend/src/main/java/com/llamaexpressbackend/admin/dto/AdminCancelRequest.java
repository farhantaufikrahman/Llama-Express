package com.llamaexpressbackend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCancelRequest(@NotBlank @Size(max = 200) String reason) {
}
