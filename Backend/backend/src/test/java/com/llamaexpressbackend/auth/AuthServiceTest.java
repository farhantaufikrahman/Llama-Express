package com.llamaexpressbackend.auth;

import com.llamaexpressbackend.auth.dto.LoginRequest;
import com.llamaexpressbackend.auth.dto.RegisterRequest;
import com.llamaexpressbackend.auth.dto.TokenResponse;
import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.UnauthorizedException;
import com.llamaexpressbackend.courier.CourierProfileRepository;
import com.llamaexpressbackend.user.Role;
import com.llamaexpressbackend.user.User;
import com.llamaexpressbackend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-16T03:00:00Z");

    @Mock
    UserRepository userRepository;
    @Mock
    CourierProfileRepository courierProfileRepository;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    TokenService tokenService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        authService = new AuthService(userRepository, courierProfileRepository, refreshTokenRepository,
                passwordEncoder, tokenService, clock);
    }

    @Test
    void registerCustomer_normalizesEmail_hashesPassword_andIssuesTokens() {
        var req = new RegisterRequest("Farhan", "  Farhan@Mail.COM ", "081234567890", "rahasia123");
        when(userRepository.existsByEmail("farhan@mail.com")).thenReturn(false);
        when(userRepository.existsByPhone("081234567890")).thenReturn(false);
        when(passwordEncoder.encode("rahasia123")).thenReturn("{bcrypt}hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        stubTokenIssuing();

        TokenResponse res = authService.registerCustomer(req);

        assertThat(res.accessToken()).isEqualTo("access-jwt");
        assertThat(res.refreshToken()).isEqualTo("raw-refresh");
        assertThat(res.tokenType()).isEqualTo("Bearer");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("farhan@mail.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("{bcrypt}hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CUSTOMER);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void registerCustomer_duplicateEmail_throwsConflict_andSavesNothing() {
        var req = new RegisterRequest("Farhan", "farhan@mail.com", "081234567890", "rahasia123");
        when(userRepository.existsByEmail("farhan@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCustomer(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized_andIssuesNoToken() {
        User user = User.create(Role.CUSTOMER, "Farhan", "farhan@mail.com", "081234567890", "{bcrypt}real");
        when(userRepository.findByEmail("farhan@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("salah", "{bcrypt}real")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("farhan@mail.com", "salah")))
                .isInstanceOf(UnauthorizedException.class);
        verify(tokenService, never()).createAccessToken(any());
    }

    @Test
    void refresh_withRevokedToken_revokesAllSessions_andThrows() {
        User user = User.create(Role.CUSTOMER, "Farhan", "farhan@mail.com", "081234567890", "{bcrypt}real");
        ReflectionTestUtils.setField(user, "id", 7L);
        RefreshToken stolen = RefreshToken.issue(user, "hash-old", NOW.plusSeconds(3600));
        stolen.revoke();

        when(tokenService.hash("old-token")).thenReturn("hash-old");
        when(refreshTokenRepository.findByTokenHash("hash-old")).thenReturn(Optional.of(stolen));

        assertThatThrownBy(() -> authService.refresh("old-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokenRepository).revokeAllActiveByUserId(7L);
        verify(tokenService, never()).createAccessToken(any());
    }

    private void stubTokenIssuing() {
        when(tokenService.createAccessToken(any())).thenReturn("access-jwt");
        when(tokenService.generateRefreshToken()).thenReturn("raw-refresh");
        when(tokenService.hash("raw-refresh")).thenReturn("hashed-refresh");
        when(tokenService.refreshTokenExpiresAt()).thenReturn(NOW.plusSeconds(7 * 24 * 3600));
        when(tokenService.accessTokenTtlSeconds()).thenReturn(900L);
    }
}