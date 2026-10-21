package com.llamaexpressbackend.user;

import com.llamaexpressbackend.auth.RefreshTokenRepository;
import com.llamaexpressbackend.common.error.BadRequestException;
import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.user.dto.ChangePasswordRequest;
import com.llamaexpressbackend.user.dto.UpdateProfileRequest;
import com.llamaexpressbackend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(find(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = find(userId);
        if (req.fullName() != null && req.fullName().isBlank()) {
            throw new BadRequestException("VALIDATION_ERROR", "Nama tidak boleh kosong");
        }
        String phone = (req.phone() == null) ? null : req.phone().trim();
        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new ConflictException("PHONE_TAKEN", "Nomor HP sudah dipakai akun lain");
        }
        user.updateProfile(req.fullName(), phone);
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = find(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            // logout otomatis"
            throw new BadRequestException("INVALID_CURRENT_PASSWORD", "Password saat ini salah");
        }
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        refreshTokenRepository.revokeAllActiveByUserId(userId); // keluarkan semua sesi lain
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User tidak ditemukan"));
    }
}