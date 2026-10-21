package com.llamaexpressbackend.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompleteDeliveryRequest(
        @NotBlank @Pattern(regexp = "^\\d{4}$", message = "Kode verifikasi harus 4 digit") String verificationCode) {
}
