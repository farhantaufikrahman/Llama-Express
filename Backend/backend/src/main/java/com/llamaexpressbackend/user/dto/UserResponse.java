package com.llamaexpressbackend.user.dto;

import com.llamaexpressbackend.user.Role;
import com.llamaexpressbackend.user.User;

public record UserResponse(Long id, String fullName, String email, String phone, Role role) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole());
    }
}