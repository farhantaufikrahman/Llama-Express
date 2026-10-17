package com.llamaexpressbackend.user;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "rahasia123";

    private record Account(String email, String phone, String accessToken, String refreshToken) {
    }

    @Test
    void updateProfile_partially_andPhoneMustStayUnique() throws Exception {
        Account me = newAccount();
        Account other = newAccount();

        // hanya nama → nomor HP tidak berubah
        mockMvc.perform(patch("/api/v1/users/me").header(AUTHORIZATION, bearer(me.accessToken()))
                .contentType(APPLICATION_JSON).content("{\"fullName\":\"Nama Baru\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nama Baru"))
                .andExpect(jsonPath("$.phone").value(me.phone()));

        // nomor HP milik akun lain → 409
        mockMvc.perform(patch("/api/v1/users/me").header(AUTHORIZATION, bearer(me.accessToken()))
                .contentType(APPLICATION_JSON).content("{\"phone\":\"" + other.phone() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_TAKEN"));

        // validasi
        mockMvc.perform(patch("/api/v1/users/me").header(AUTHORIZATION, bearer(me.accessToken()))
                .contentType(APPLICATION_JSON).content("{\"fullName\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/users/me").header(AUTHORIZATION, bearer(me.accessToken()))
                .contentType(APPLICATION_JSON).content("{\"phone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void changePassword_requiresCurrentPassword_andRevokesOtherSessions() throws Exception {
        Account me = newAccount();

        mockMvc.perform(post("/api/v1/users/me/password").header(AUTHORIZATION, bearer(me.accessToken()))
                .contentType(APPLICATION_JSON)
                .content("{\"currentPassword\":\"salahbanget\",\"newPassword\":\"passwordBaru1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        mockMvc.perform(post("/api/v1/users/me/password").header(AUTHORIZATION, bearer(me.accessToken()))
                .contentType(APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"passwordBaru1\"}"))
                .andExpect(status().isNoContent());

        // refresh token lama sudah dicabut
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + me.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());

        // password lama ditolak, password baru diterima
        mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + me.email() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        login(me.email(), "passwordBaru1"); // helper base class — memastikan 200
    }

    private Account newAccount() throws Exception {
        String email = "profile-" + UUID.randomUUID() + "@test.com";
        String phone = randomPhone();
        MvcResult r = mockMvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON)
                .content("""
                        {"fullName":"Profil Test","email":"%s","phone":"%s","password":"%s"}
                        """.formatted(email, phone, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        return new Account(email, phone, read(r, "$.accessToken"), read(r, "$.refreshToken"));
    }
}