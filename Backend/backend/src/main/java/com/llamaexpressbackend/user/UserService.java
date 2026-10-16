package com.llamaexpressbackend.user;

import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User tidak ditemukan"));
    }
}