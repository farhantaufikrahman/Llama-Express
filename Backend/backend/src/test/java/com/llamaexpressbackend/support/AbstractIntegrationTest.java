package com.llamaexpressbackend.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    /** Daftarkan customer baru (data acak) dan kembalikan access token-nya */
    protected String registerCustomer() throws Exception {
        String body = """
                {"fullName":"Test Customer","email":"cust-%s@test.com","phone":"%s","password":"rahasia123"}
                """.formatted(UUID.randomUUID(), randomPhone());
        MvcResult r = mockMvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return read(r, "$.accessToken");
    }

    protected static String orderJson(String paymentMethod, String weightKg) {
        return """
                {"serviceType":"INSTANT","vehicleType":"MOTOR","itemCategory":"DOCUMENT",
                 "itemDesc":"Berkas kontrak","weightKg":%s,
                 "pickup":{"address":"Titik A","lat":-6.90,"lng":107.60},
                 "dropoff":{"address":"Titik B","lat":-6.91,"lng":107.60},
                 "recipientName":"Budi","recipientPhone":"081298765432","notes":"Titip resepsionis",
                 "paymentMethod":"%s"}
                """.formatted(weightKg, paymentMethod);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static String randomPhone() {
        return "0812" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    protected static <T> T read(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    protected static final String ADMIN_EMAIL = "admin@test.local";
    protected static final String ADMIN_PASSWORD = "admin12345";

    protected String login(String email, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return read(r, "$.accessToken");
    }

    protected String loginAdmin() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /** Daftarkan kurir baru (belum diverifikasi) dan kembalikan access token-nya */
    protected String registerCourier(String vehicleType) throws Exception {
        String body = """
                {"fullName":"Test Courier","email":"courier-%s@test.com","phone":"%s","password":"rahasia123",
                 "vehicleType":"%s","plateNumber":"%s"}
                """.formatted(UUID.randomUUID(), randomPhone(), vehicleType, randomPlate());
        MvcResult r = mockMvc.perform(post("/api/v1/auth/register/courier").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return read(r, "$.accessToken");
    }

    protected static String randomPlate() {
        var rnd = ThreadLocalRandom.current();
        return "D" + rnd.nextInt(1000, 10000)
                + (char) ('A' + rnd.nextInt(26)) + (char) ('A' + rnd.nextInt(26)) + (char) ('A' + rnd.nextInt(26));
    }

}