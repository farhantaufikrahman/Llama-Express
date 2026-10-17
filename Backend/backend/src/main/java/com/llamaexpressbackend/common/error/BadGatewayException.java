package com.llamaexpressbackend.common.error;

import org.springframework.http.HttpStatus;

public class BadGatewayException extends ApiException {
    public BadGatewayException(String code, String message) {
        super(HttpStatus.BAD_GATEWAY, code, message);
    }
}