package com.llamaexpressbackend.dispatch;

import com.jayway.jsonpath.JsonPath;
import com.llamaexpressbackend.courier.CourierLocationStore;
import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Setiap test memakai lokasi berbeda (berjauhan > 10 km) supaya kurir dari test
 * lain
 * tidak ikut mendapat tawaran — data Redis/DB dipakai bersama oleh semua test.
 */
class DispatchFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String OFFERS = "/api/v1/courier/offers";

    @Autowired
    CourierLocationStore locationStore;

    @Test
    void nearbyCourier_receivesOffer_accepts_andBecomesBusy() throws Exception {
        double lat = -7.50, lng = 110.40;
        TestCourier courier = verifiedOnlineCourier("MOTOR", lat, lng);
        String customerToken = registerCustomer();
        String code = createCashOrderAt(customerToken, lat, lng);

        Long offerId = awaitOfferId(courier, code); // Kafka → listener → dispatch → offer

        mockMvc.perform(post(OFFERS + "/" + offerId + "/accept").header(AUTHORIZATION, bearer(courier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.verificationCode").doesNotExist());

        mockMvc.perform(get("/api/v1/orders/" + code).header(AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        // sibuk → tidak muncul di pencarian, walau tetap mengirim lokasi
        sendLocation(courier, lat, lng);
        assertThat(locationStore.findNearby(VehicleType.MOTOR, lat, lng, 1.0, 50))
                .noneMatch(c -> c.courierId().equals(courier.id()));

        // accept dua kali → 409
        mockMvc.perform(post(OFFERS + "/" + offerId + "/accept").header(AUTHORIZATION, bearer(courier.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OFFER_NOT_AVAILABLE"));
    }

    @Test
    void twoCouriersAcceptSameOrderAtOnce_exactlyOneWins() throws Exception {
        double lat = -7.80, lng = 110.36;
        TestCourier a = verifiedOnlineCourier("MOTOR", lat, lng);
        TestCourier b = verifiedOnlineCourier("MOTOR", lat + 0.001, lng);
        String customerToken = registerCustomer();
        String code = createCashOrderAt(customerToken, lat, lng);

        Long offerA = awaitOfferId(a, code);
        Long offerB = awaitOfferId(b, code);

        // dua thread menekan "Terima" di saat yang sama
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        Future<Integer> fa = pool.submit(() -> {
            startSignal.await();
            return accept(a, offerA);
        });
        Future<Integer> fb = pool.submit(() -> {
            startSignal.await();
            return accept(b, offerB);
        });
        startSignal.countDown();
        List<Integer> statuses = List.of(fa.get(20, TimeUnit.SECONDS), fb.get(20, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        mockMvc.perform(get("/api/v1/orders/" + code).header(AUTHORIZATION, bearer(customerToken)))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void courier_canRejectOffer() throws Exception {
        double lat = -7.60, lng = 110.55;
        TestCourier courier = verifiedOnlineCourier("MOTOR", lat, lng);
        String code = createCashOrderAt(registerCustomer(), lat, lng);
        Long offerId = awaitOfferId(courier, code);

        mockMvc.perform(post(OFFERS + "/" + offerId + "/reject").header(AUTHORIZATION, bearer(courier.token())))
                .andExpect(status().isNoContent());

        MvcResult r = mockMvc.perform(get(OFFERS).header(AUTHORIZATION, bearer(courier.token())))
                .andExpect(status().isOk()).andReturn();
        List<Object> remaining = JsonPath.read(r.getResponse().getContentAsString(),
                "$[?(@.orderCode == '" + code + "')]");
        assertThat(remaining).isEmpty();

        mockMvc.perform(post(OFFERS + "/" + offerId + "/reject").header(AUTHORIZATION, bearer(courier.token())))
                .andExpect(status().isConflict());
    }

    private int accept(TestCourier courier, Long offerId) throws Exception {
        return mockMvc.perform(post(OFFERS + "/" + offerId + "/accept").header(AUTHORIZATION, bearer(courier.token())))
                .andReturn().getResponse().getStatus();
    }
}