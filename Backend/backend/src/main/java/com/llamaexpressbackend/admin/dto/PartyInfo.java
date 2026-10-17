package com.llamaexpressbackend.admin.dto;

import com.llamaexpressbackend.user.User;

public record PartyInfo(Long id, String fullName, String email, String phone) {
    public static PartyInfo from(User u) {
        return (u == null) ? null : new PartyInfo(u.getId(), u.getFullName(), u.getEmail(), u.getPhone());
    }
}
