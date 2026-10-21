package com.llamaexpressbackend.auth;

import com.llamaexpressbackend.common.error.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {
    public InvalidRefreshTokenException() {
        super("INVALID_REFRESH_TOKEN", "Refresh token tidak valid atau kedaluwarsa");
    }
}