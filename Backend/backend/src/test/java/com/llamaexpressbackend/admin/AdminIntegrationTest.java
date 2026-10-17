package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminIntegrationTest extends AbstractIntegrationTest {

    private record Customer(String token, String email, String phone) {
    }

    @Test
    void dashboard_showsTodaysNumbers_andIsAdminOnly() throws Exception {
        Customer c = newCustomer();
        createCashOrderAt(c.token(), -5.90, 106.10);

        mockMvc.perform(get("/api/v1/admin/dashboard/summary").header(AUTHORIZATION, bearer(loginAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersToday").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.statusBreakdown").isMap());

        mockMvc.perform(get("/api/v1/admin/dashboard/summary").header(AUTHORIZATION, bearer(c.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void search_byCustomerPhone_orderCode_andStatus() throws Exception {
        Customer c = newCustomer();
        String code1 = createCashOrderAt(c.token(), -5.91, 106.11);
        createOrderAt(c.token(), -5.92, 106.12, "MIDTRANS");
        String admin = loginAdmin();

        mockMvc.perform(get("/api/v1/admin/orders").param("q", c.phone()).header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].customerName").value("Admin Test Customer"));

        mockMvc.perform(get("/api/v1/admin/orders").param("q", code1).header(AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/admin/orders").param("q", c.phone()).param("paymentMethod", "MIDTRANS")
                .header(AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CREATED"));
    }

    @Test
    void detail_andAdminCancel() throws Exception {
        Customer c = newCustomer();
        String code = createOrderAt(c.token(), -5.93, 106.13, "MIDTRANS"); // tetap CREATED (belum dibayar)
        String admin = loginAdmin();

        mockMvc.perform(get("/api/v1/admin/orders/" + code).header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.email").value(c.email()))
                .andExpect(jsonPath("$.order.code").value(code));

        mockMvc.perform(post("/api/v1/admin/orders/" + code + "/cancel").header(AUTHORIZATION, bearer(admin))
                .contentType(APPLICATION_JSON).content("{\"reason\":\"Permintaan customer via CS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("CANCELLED"))
                .andExpect(jsonPath("$.order.timeline[-1].note").value("Dibatalkan admin: Permintaan customer via CS"));

        mockMvc.perform(post("/api/v1/admin/orders/" + code + "/cancel").header(AUTHORIZATION, bearer(admin))
                .contentType(APPLICATION_JSON).content("{\"reason\":\"Lagi\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void csvExport_streamsMatchingOrders() throws Exception {
        Customer c = newCustomer();
        String code1 = createCashOrderAt(c.token(), -5.94, 106.14);
        String code2 = createCashOrderAt(c.token(), -5.95, 106.15);

        // StreamingResponseBody = request async → MockMvc perlu asyncDispatch
        MvcResult started = mockMvc.perform(get("/api/v1/admin/orders/export").param("q", c.phone())
                .header(AUTHORIZATION, bearer(loginAdmin())))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult done = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
                .andReturn();

        String csv = done.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("order_code,created_at,status").contains(code1).contains(code2);
        assertThat(csv.lines().count()).isEqualTo(3); // header + 2 order
    }

    @Test
    void tariffs_newTariffClosesPrevious_noBackdating_noOverlap() throws Exception {
        String admin = loginAdmin();
        // SAME_DAY + CAR & berlaku BESOK → tidak mengubah harga yang dipakai test lain
        String tomorrow = OffsetDateTime.now(ZoneOffset.ofHours(7)).plusDays(1).toString();

        mockMvc.perform(post("/api/v1/admin/tariffs").header(AUTHORIZATION, bearer(admin)).contentType(APPLICATION_JSON)
                .content(tariffJson(tomorrow)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(false)); // terjadwal, belum berlaku

        mockMvc.perform(post("/api/v1/admin/tariffs").header(AUTHORIZATION, bearer(admin)).contentType(APPLICATION_JSON)
                .content(tariffJson(OffsetDateTime.now(ZoneOffset.ofHours(7)).toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TARIFF_OVERLAP"));

        mockMvc.perform(post("/api/v1/admin/tariffs").header(AUTHORIZATION, bearer(admin)).contentType(APPLICATION_JSON)
                .content(tariffJson("2020-01-01T00:00:00+07:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVE_FROM_IN_PAST"));

        mockMvc.perform(get("/api/v1/admin/tariffs").header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.serviceType == 'SAME_DAY' && @.vehicleType == 'CAR' && @.activeTo != null)]")
                        .exists());
    }

    @Test
    void suspendedCustomer_cannotLogin_untilActivated() throws Exception {
        Customer c = newCustomer();
        String admin = loginAdmin();
        MvcResult me = mockMvc.perform(get("/api/v1/users/me").header(AUTHORIZATION, bearer(c.token()))).andReturn();
        Number userId = read(me, "$.id");

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/suspend").header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + c.email() + "\",\"password\":\"rahasia123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/activate").header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
        login(c.email(), "rahasia123"); // 200 lagi
    }

    // ---------- helper khusus test ini ----------

    private Customer newCustomer() throws Exception {
        String email = "admin-test-" + UUID.randomUUID() + "@test.com";
        String phone = randomPhone();
        MvcResult r = mockMvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON)
                .content("""
                        {"fullName":"Admin Test Customer","email":"%s","phone":"%s","password":"rahasia123"}
                        """.formatted(email, phone)))
                .andExpect(status().isCreated()).andReturn();
        return new Customer(read(r, "$.accessToken"), email, phone);
    }

    private static String tariffJson(String activeFrom) {
        return """
                {"serviceType":"SAME_DAY","vehicleType":"CAR","baseFare":16000,"perKmFare":3600,"minFare":21000,
                 "activeFrom":"%s"}
                """.formatted(activeFrom);
    }
}