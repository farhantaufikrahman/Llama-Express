package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.AdminUserResponse;
import com.llamaexpressbackend.auth.RefreshTokenRepository;
import com.llamaexpressbackend.common.error.BadRequestException;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.common.web.PageResponse;
import com.llamaexpressbackend.courier.CourierLocationStore;
import com.llamaexpressbackend.courier.CourierProfileRepository;
import com.llamaexpressbackend.user.Role;
import com.llamaexpressbackend.user.User;
import com.llamaexpressbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CourierProfileRepository profileRepository;
    private final CourierLocationStore locationStore;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(Role role, String q, int page, int size) {
        String keyword = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT); // Oracle: '' = NULL
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(userRepository.search(role, keyword, pageable).map(AdminUserResponse::from));
    }

    @Transactional
    public AdminUserResponse suspend(Long userId) {
        User user = find(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("CANNOT_SUSPEND_ADMIN", "Akun admin tidak bisa dibekukan dari sini");
        }
        user.suspend();
        refreshTokenRepository.revokeAllActiveByUserId(userId); // flushAutomatically=true → suspend() ikut tersimpan
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            if (profile.isOnline()) {
                profile.goOffline();
                locationStore.remove(userId, profile.getVehicleType());
            }
        });
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse activate(Long userId) {
        User user = find(userId);
        user.activate();
        return AdminUserResponse.from(user);
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User tidak ditemukan"));
    }
}