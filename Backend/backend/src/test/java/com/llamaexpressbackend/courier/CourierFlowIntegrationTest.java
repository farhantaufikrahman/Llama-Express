package com.llamaexpressbackend.courier;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourierFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String ME = "/api/v1/courier/me";

    @Autowired
    CourierLocationStore locationStore;

    @Test
    void courier_mustBeVerified_thenGoesOnline_andIsFoundNearby() throws Exception {
        String courierToken = registerCourier("MOTOR");

        // 1. profil awal: PENDING & offline
        MvcResult me = mockMvc.perform(get(ME).header(AUTHORIZATION, bearer(courierToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.online").value(false))
                .andReturn();
        Number idNumber = read(me, "$.userId"); // JsonPath bisa mengembalikan Integer atau Long
        Long courierId = idNumber.longValue();

        // 2. belum diverifikasi → tidak boleh online
        mockMvc.perform(patch(ME + "/status").header(AUTHORIZATION, bearer(courierToken))
                .contentType(APPLICATION_JSON).content("{\"online\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COURIER_NOT_VERIFIED"));

        // 3. admin memverifikasi
        mockMvc.perform(
                patch("/api/v1/admin/couriers/" + courierId + "/verify").header(AUTHORIZATION, bearer(loginAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        // 4. online + kirim lokasi
        mockMvc.perform(patch(ME + "/status").header(AUTHORIZATION, bearer(courierToken))
                .contentType(APPLICATION_JSON).content("{\"online\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.online").value(true));

        mockMvc.perform(post(ME + "/location").header(AUTHORIZATION, bearer(courierToken))
                .contentType(APPLICATION_JSON).content("{\"lat\":-6.90,\"lng\":107.60}"))
                .andExpect(status().isNoContent());

        // 5. ditemukan dari titik ±1.1 km jauhnya (hanya di himpunan MOTOR)
        List<NearbyCourier> motors = locationStore.findNearby(VehicleType.MOTOR, -6.91, 107.60, 2.0, 50);
        assertThat(motors).anySatisfy(c -> {
            assertThat(c.courierId()).isEqualTo(courierId);
            assertThat(c.distanceKm()).isBetween(1.0, 1.2);
        });
        assertThat(locationStore.findNearby(VehicleType.CAR, -6.91, 107.60, 2.0, 50))
                .noneMatch(c -> c.courierId().equals(courierId));

        // 6. offline → hilang dari pencarian
        mockMvc.perform(patch(ME + "/status").header(AUTHORIZATION, bearer(courierToken))
                .contentType(APPLICATION_JSON).content("{\"online\":false}"))
                .andExpect(status().isOk());
        assertThat(locationStore.findNearby(VehicleType.MOTOR, -6.91, 107.60, 2.0, 50))
                .noneMatch(c -> c.courierId().equals(courierId));

        // 7. kirim lokasi saat offline → 409
        mockMvc.perform(post(ME + "/location").header(AUTHORIZATION, bearer(courierToken))
                .contentType(APPLICATION_JSON).content("{\"lat\":-6.90,\"lng\":107.60}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURIER_OFFLINE"));
    }

    @Test
    void customer_cannotAccessCourierEndpoints() throws Exception {
        mockMvc.perform(get(ME).header(AUTHORIZATION, bearer(registerCustomer())))
                .andExpect(status().isForbidden());
    }

    @Test
    void courier_cannotVerifyThemselves() throws Exception {
        mockMvc.perform(
                patch("/api/v1/admin/couriers/1/verify").header(AUTHORIZATION, bearer(registerCourier("MOTOR"))))
                .andExpect(status().isForbidden());
    }
}