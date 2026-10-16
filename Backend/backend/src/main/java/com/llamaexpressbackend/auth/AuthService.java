package com.llamaexpressbackend.auth;

import com.llamaexpressbackend.auth.dto.*;
import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.ForbiddenException;
import com.llamaexpressbackend.common.error.UnauthorizedException;
import com.llamaexpressbackend.courier.CourierProfile;
import com.llamaexpressbackend.courier.CourierProfileRepository;
import com.llamaexpressbackend.user.Role;
import com.llamaexpressbackend.user.User;
import com.llamaexpressbackend.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
            CourierProfileRepository courierProfileRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            Clock clock) {
        this.userRepository = userRepository;
        this.courierProfileRepository = courierProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        // dipakai saat email tidak ditemukan, supaya waktu respons sama → penyerang tak
        // bisa menebak email terdaftar
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public TokenResponse registerCustomer(RegisterRequest req) {
        User user = createUser(Role.CUSTOMER, req.fullName(), req.email(), req.phone(), req.password());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse registerCourier(RegisterCourierRequest req) {
        if (courierProfileRepository.existsByPlateNumber(CourierProfile.normalizePlate(req.plateNumber()))) {
            throw new ConflictException("PLATE_TAKEN", "Nomor plat sudah terdaftar");
        }
        User user = createUser(Role.COURIER, req.fullName(), req.email(), req.phone(), req.password());
        courierProfileRepository
                .save(CourierProfile.create(user, req.vehicleType(), req.plateNumber(), req.licenseNo()));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalizeEmail(req.email())).orElse(null);
        String hash = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(req.password(), hash);

        if (user == null || !passwordOk) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Email atau password salah");
        }
        if (!user.isActive()) {
            throw new ForbiddenException("ACCOUNT_SUSPENDED", "Akun kamu dinonaktifkan");
        }
        return issueTokens(user);
    }

    /**
     * Rotation + reuse detection: setiap refresh token hanya boleh dipakai SEKALI.
     * Kalau token yang sudah dicabut dipakai lagi → kemungkinan dicuri → cabut
     * semua sesi user.
     * noRollbackFor: supaya pencabutan massal tetap tersimpan walau method melempar
     * exception.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(tokenService.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}", current.getUser().getId());
            refreshTokenRepository.revokeAllActiveByUserId(current.getUser().getId());
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(clock.instant())) {
            throw new InvalidRefreshTokenException();
        }

        User user = current.getUser();
        if (!user.isActive()) {
            throw new ForbiddenException("ACCOUNT_SUSPENDED", "Akun kamu dinonaktifkan");
        }
        current.revoke(); // tidak perlu save(): JPA otomatis menyimpan perubahan saat transaksi commit
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(tokenService.hash(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    // ---------- helpers ----------

    private User createUser(Role role, String fullName, String email, String phone, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = phone.trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("EMAIL_TAKEN", "Email sudah terdaftar");
        }
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new ConflictException("PHONE_TAKEN", "Nomor HP sudah terdaftar");
        }
        User user = User.create(role, fullName, normalizedEmail, normalizedPhone, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = tokenService.createAccessToken(user);
        String rawRefresh = tokenService.generateRefreshToken();
        refreshTokenRepository.save(
                RefreshToken.issue(user, tokenService.hash(rawRefresh), tokenService.refreshTokenExpiresAt()));
        return TokenResponse.bearer(accessToken, rawRefresh, tokenService.accessTokenTtlSeconds());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}