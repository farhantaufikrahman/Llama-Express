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

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
                                """
                                .formatted(UUID.randomUUID(), randomPhone());
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
                                """
                                .formatted(UUID.randomUUID(), randomPhone(), vehicleType, randomPlate());
                MvcResult r = mockMvc
                                .perform(post("/api/v1/auth/register/courier").contentType(APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isCreated())
                                .andReturn();
                return read(r, "$.accessToken");
        }

        protected static String randomPlate() {
                var rnd = ThreadLocalRandom.current();
                return "D" + rnd.nextInt(1000, 10000)
                                + (char) ('A' + rnd.nextInt(26)) + (char) ('A' + rnd.nextInt(26))
                                + (char) ('A' + rnd.nextInt(26));
        }

        protected record TestCourier(String token, Long id) {
        }

        /** Kurir yang sudah diverifikasi admin, online, dan berada di (lat, lng) */
        protected TestCourier verifiedOnlineCourier(String vehicleType, double lat, double lng) throws Exception {
                String token = registerCourier(vehicleType);
                MvcResult me = mockMvc.perform(get("/api/v1/courier/me").header(AUTHORIZATION, bearer(token)))
                                .andExpect(status().isOk()).andReturn();
                Number id = read(me, "$.userId");

                mockMvc.perform(patch("/api/v1/admin/couriers/" + id + "/verify").header(AUTHORIZATION,
                                bearer(loginAdmin())))
                                .andExpect(status().isOk());
                mockMvc.perform(patch("/api/v1/courier/me/status").header(AUTHORIZATION, bearer(token))
                                .contentType(APPLICATION_JSON).content("{\"online\":true}"))
                                .andExpect(status().isOk());

                TestCourier courier = new TestCourier(token, id.longValue());
                sendLocation(courier, lat, lng);
                return courier;
        }

        protected void sendLocation(TestCourier courier, double lat, double lng) throws Exception {
                mockMvc.perform(post("/api/v1/courier/me/location").header(AUTHORIZATION, bearer(courier.token()))
                                .contentType(APPLICATION_JSON)
                                .content(String.format(Locale.ROOT, "{\"lat\":%.6f,\"lng\":%.6f}", lat, lng)))
                                .andExpect(status().isNoContent());
        }

        /** Order CASH dengan titik jemput di (lat, lng) → langsung SEARCHING_COURIER */
        protected String createCashOrderAt(String customerToken, double lat, double lng) throws Exception {
                String body = String.format(Locale.ROOT,
                                """
                                                {"serviceType":"INSTANT","vehicleType":"MOTOR","itemCategory":"DOCUMENT","itemDesc":"Berkas","weightKg":1.5,
                                                 "pickup":{"address":"Titik Jemput","lat":%.6f,"lng":%.6f},
                                                 "dropoff":{"address":"Titik Antar","lat":%.6f,"lng":%.6f},
                                                 "recipientName":"Budi","recipientPhone":"081298765432","paymentMethod":"CASH"}
                                                """,
                                lat, lng, lat - 0.01, lng);
                MvcResult r = mockMvc.perform(post("/api/v1/orders").header(AUTHORIZATION, bearer(customerToken))
                                .contentType(APPLICATION_JSON).content(body))
                                .andExpect(status().isCreated()).andReturn();
                return read(r, "$.code");
        }

        /**
         * Tunggu sampai kurir menerima tawaran untuk order tertentu (dispatch berjalan
         * asinkron lewat Kafka)
         */
        protected Long awaitOfferId(TestCourier courier, String orderCode) throws Exception {
                long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
                while (System.nanoTime() < deadline) {
                        MvcResult r = mockMvc
                                        .perform(get("/api/v1/courier/offers").header(AUTHORIZATION,
                                                        bearer(courier.token())))
                                        .andExpect(status().isOk()).andReturn();
                        List<Number> ids = JsonPath.read(r.getResponse().getContentAsString(),
                                        "$[?(@.orderCode == '" + orderCode + "')].offerId");
                        if (!ids.isEmpty()) {
                                return ids.getFirst().longValue();
                        }
                        Thread.sleep(300);
                }
                throw new AssertionError("Kurir tidak menerima tawaran untuk " + orderCode + " dalam 15 detik");
        }

}