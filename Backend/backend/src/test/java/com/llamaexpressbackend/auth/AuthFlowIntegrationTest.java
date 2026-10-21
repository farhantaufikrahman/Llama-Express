package com.llamaexpressbackend.auth;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/auth";

    @Test
    void fullFlow_register_login_me_refreshRotation_reuseDetection() throws Exception {
        String email = uniqueEmail();

        // 1. register → 201
        mockMvc.perform(post(BASE + "/register").contentType(APPLICATION_JSON)
                .content(registerJson(email, uniquePhone(), "rahasia123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        // 2. login → 200 (email huruf besar tetap dikenali karena dinormalisasi)
        MvcResult login = mockMvc.perform(post(BASE + "/login").contentType(APPLICATION_JSON)
                .content(loginJson(email.toUpperCase(), "rahasia123")))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = read(login, "$.accessToken");
        String refreshToken = read(login, "$.refreshToken");

        // 3. /users/me dengan token → 200
        mockMvc.perform(get("/api/v1/users/me").header(AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // 4. refresh → 200, dapat pasangan token baru
        MvcResult refreshed = mockMvc.perform(post(BASE + "/refresh").contentType(APPLICATION_JSON)
                .content(refreshJson(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        String newRefreshToken = read(refreshed, "$.refreshToken");

        // 5. pakai refresh token LAMA lagi → 401 (reuse detection)
        mockMvc.perform(post(BASE + "/refresh").contentType(APPLICATION_JSON)
                .content(refreshJson(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // 6. akibatnya, refresh token BARU pun ikut dicabut → 401
        mockMvc.perform(post(BASE + "/refresh").contentType(APPLICATION_JSON)
                .content(refreshJson(newRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post(BASE + "/register").contentType(APPLICATION_JSON)
                .content(registerJson(email, uniquePhone(), "rahasia123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE + "/register").contentType(APPLICATION_JSON)
                .content(registerJson(email, uniquePhone(), "rahasia123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void register_invalidPayload_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post(BASE + "/register").contentType(APPLICATION_JSON)
                .content(registerJson("bukan-email", "12345", "pendek")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post(BASE + "/register").contentType(APPLICATION_JSON)
                .content(registerJson(email, uniquePhone(), "rahasia123")));

        mockMvc.perform(post(BASE + "/login").contentType(APPLICATION_JSON)
                .content(loginJson(email, "passwordsalah")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withTamperedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void courierEndpoint_withCustomerToken_returns403() throws Exception {
        MvcResult reg = mockMvc.perform(post(BASE + "/register").contentType(APPLICATION_JSON)
                .content(registerJson(uniqueEmail(), uniquePhone(), "rahasia123")))
                .andReturn();

        // endpoint kurir belum dibuat, tapi aturan hasRole("COURIER") di SecurityConfig
        // sudah menolak duluan
        mockMvc.perform(get("/api/v1/courier/offers")
                .header(AUTHORIZATION, "Bearer " + read(reg, "$.accessToken")))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private static String uniquePhone() {
        return "0812" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private static String registerJson(String email, String phone, String password) {
        return """
                {"fullName":"Test User","email":"%s","phone":"%s","password":"%s"}
                """.formatted(email, phone, password);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private static String refreshJson(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }
}