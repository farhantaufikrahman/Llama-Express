package com.llamaexpressbackend.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (props.email() == null || props.email().isBlank() || props.password() == null
                || props.password().isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD tidak di-set — lewati pembuatan admin awal");
            return;
        }
        String email = props.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return; // sudah ada → tidak melakukan apa-apa (aman dijalankan berulang)
        }
        userRepository.save(User.create(Role.ADMIN, "Administrator", email, props.phone(),
                passwordEncoder.encode(props.password())));
        log.info("Admin awal dibuat: {}", email);
    }
}