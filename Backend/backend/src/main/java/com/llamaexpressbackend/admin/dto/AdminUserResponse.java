package com.llamaexpressbackend.admin.dto;

import com.llamaexpressbackend.user.Role;
import com.llamaexpressbackend.user.User;
import com.llamaexpressbackend.user.UserStatus;
import java.time.Instant;

public record AdminUserResponse(Long id, String fullName, String email, String phone, Role role,
        UserStatus status, Instant createdAt) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getStatus(), u.getCreatedAt());
    }
}