package com.llamaexpressbackend.delivery;

import com.llamaexpressbackend.courier.CourierLocationStore;
import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lokasi tiap test berjauhan (> 20 km) dari test dispatch lain supaya tidak
 * saling mengambil tawaran
 */
class DeliveryLifecycleIntegrationTest extends AbstractIntegrationTest {

    private static final String JOBS = "/api/v1/courier/jobs";
    private static final byte[] FAKE_JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10 };

    @Autowired
    CourierLocationStore locationStore;

    private record Delivery(TestCourier courier, String customerToken, String code) {
    }

    @Test
    void fullDelivery_fromPickupToRating() throws Exception {
        double lat = -7.30, lng = 110.70;
        Delivery d = inTransitDelivery(lat, lng);

        mockMvc.perform(get(JOBS + "/active").header(AUTHORIZATION, bearer(d.courier().token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(d.code()))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));

        // customer tidak bisa membatalkan setelah paket diambil
        mockMvc.perform(post("/api/v1/orders/" + d.code() + "/cancel").header(AUTHORIZATION, bearer(d.customerToken()))
                .contentType(APPLICATION_JSON).content("{\"reason\":\"Batal\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_CANCELLABLE"));

        // selesai tanpa foto → 409
        String realCode = verificationCodeOf(d);
        complete(d, realCode)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROOF_REQUIRED"));

        // file bukan gambar → 400, lalu foto valid → 200
        uploadProof(d, "text/plain", "bukan foto".getBytes())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"));
        uploadProof(d, "image/jpeg", FAKE_JPEG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl", startsWith("fake://")))
                .andExpect(jsonPath("$.receivedBy").value("Budi"));

        // kode salah → 400, kode benar → DELIVERED
        complete(d, wrongCode(realCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"));
        complete(d, realCode)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        mockMvc.perform(get("/api/v1/orders/" + d.code()).header(AUTHORIZATION, bearer(d.customerToken())))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        // kurir tidak punya job aktif dan kembali tersedia
        mockMvc.perform(get(JOBS + "/active").header(AUTHORIZATION, bearer(d.courier().token())))
                .andExpect(status().isNoContent());
        sendLocation(d.courier(), lat, lng);
        assertThat(locationStore.findNearby(VehicleType.MOTOR, lat, lng, 1.0, 50))
                .anyMatch(c -> c.courierId().equals(d.courier().id()));

        // rating sekali saja
        rate(d, 5)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courierRatingAvg").value(5.0));
        rate(d, 4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_RATED"));
    }

    @Test
    void fiveWrongCodes_lockCompletion() throws Exception {
        Delivery d = inTransitDelivery(-7.10, 110.90);
        uploadProof(d, "image/jpeg", FAKE_JPEG).andExpect(status().isOk());
        String realCode = verificationCodeOf(d);

        for (int i = 0; i < 5; i++) {
            complete(d, wrongCode(realCode)).andExpect(status().isBadRequest());
        }
        // kode BENAR pun ditolak selama terkunci
        complete(d, realCode)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
    }

    @Test
    void cannotRate_beforeDelivered() throws Exception {
        String customerToken = registerCustomer();
        String code = createCashOrderAt(customerToken, -6.20, 106.80);

        mockMvc.perform(post("/api/v1/orders/" + code + "/rating").header(AUTHORIZATION, bearer(customerToken))
                .contentType(APPLICATION_JSON).content("{\"score\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_DELIVERED"));
    }

    // ---------- helper khusus test ini ----------

    /** Kurir menerima order, menjemput, dan paket sudah dalam perjalanan */
    private Delivery inTransitDelivery(double lat, double lng) throws Exception {
        TestCourier courier = verifiedOnlineCourier("MOTOR", lat, lng);
        String customerToken = registerCustomer();
        String code = createCashOrderAt(customerToken, lat, lng);
        Long offerId = awaitOfferId(courier, code);

        mockMvc.perform(
                post("/api/v1/courier/offers/" + offerId + "/accept").header(AUTHORIZATION, bearer(courier.token())))
                .andExpect(status().isOk());
        mockMvc.perform(post(JOBS + "/" + code + "/start-pickup").header(AUTHORIZATION, bearer(courier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKING_UP"));
        mockMvc.perform(post(JOBS + "/" + code + "/pickup").header(AUTHORIZATION, bearer(courier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
        return new Delivery(courier, customerToken, code);
    }

    private String verificationCodeOf(Delivery d) throws Exception {
        MvcResult r = mockMvc
                .perform(get("/api/v1/orders/" + d.code()).header(AUTHORIZATION, bearer(d.customerToken())))
                .andExpect(status().isOk()).andReturn();
        return read(r, "$.verificationCode");
    }

    private static String wrongCode(String realCode) {
        return realCode.equals("0000") ? "1111" : "0000";
    }

    private ResultActions uploadProof(Delivery d, String contentType, byte[] content) throws Exception {
        return mockMvc.perform(multipart(JOBS + "/" + d.code() + "/proof")
                .file(new MockMultipartFile("photo", "proof.jpg", contentType, content))
                .param("receivedBy", "Budi")
                .header(AUTHORIZATION, bearer(d.courier().token())));
    }

    private ResultActions complete(Delivery d, String verificationCode) throws Exception {
        return mockMvc
                .perform(post(JOBS + "/" + d.code() + "/complete").header(AUTHORIZATION, bearer(d.courier().token()))
                        .contentType(APPLICATION_JSON).content("{\"verificationCode\":\"" + verificationCode + "\"}"));
    }

    private ResultActions rate(Delivery d, int score) throws Exception {
        return mockMvc.perform(post("/api/v1/orders/" + d.code() + "/rating")
                .header(AUTHORIZATION, bearer(d.customerToken()))
                .contentType(APPLICATION_JSON).content("{\"score\":" + score + ",\"review\":\"Cepat dan ramah\"}"));
    }
}