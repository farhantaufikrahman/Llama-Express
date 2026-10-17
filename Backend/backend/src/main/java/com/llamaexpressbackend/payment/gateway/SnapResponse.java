package com.llamaexpressbackend.payment.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SnapResponse(String token, @JsonProperty("redirect_url") String redirectUrl) {
}
