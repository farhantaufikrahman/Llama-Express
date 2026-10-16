package com.llamaexpressbackend.order;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String ORDERS = "/api/v1/orders";

    private static final String QUOTE_JSON = """
            {"serviceType":"INSTANT","vehicleType":"MOTOR",
             "pickup":{"address":"Titik A","lat":-6.90,"lng":107.60},
             "dropoff":{"address":"Titik B","lat":-6.91,"lng":107.60}}
            """;

    @Test
    void customer_canQuote_create_list_view_andCancel() throws Exception {
        String token = registerCustomer();

        // quote — memakai tarif dari migration V2
        mockMvc.perform(post(ORDERS + "/quote").header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content(QUOTE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceMeters").value(1446))
                .andExpect(jsonPath("$.total").value(12000));

        // create (CASH) → langsung SEARCHING_COURIER, timeline 2 entri
        MvcResult created = mockMvc.perform(post(ORDERS).header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content(orderJson("CASH", "1.5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", startsWith("LX-")))
                .andExpect(jsonPath("$.status").value("SEARCHING_COURIER"))
                .andExpect(jsonPath("$.priceTotal").value(12000))
                .andExpect(jsonPath("$.verificationCode", matchesPattern("\\d{4}")))
                .andExpect(jsonPath("$.timeline.length()").value(2))
                .andReturn();
        String code = read(created, "$.code");

        // list
        mockMvc.perform(get(ORDERS).header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value(code));

        // detail
        mockMvc.perform(get(ORDERS + "/" + code).header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.pickup.address").value("Titik A"));

        // cancel → CANCELLED
        mockMvc.perform(post(ORDERS + "/" + code + "/cancel").header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content("{\"reason\":\"Salah alamat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Salah alamat"))
                .andExpect(jsonPath("$.timeline.length()").value(3));

        // cancel lagi → 409
        mockMvc.perform(post(ORDERS + "/" + code + "/cancel").header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content("{\"reason\":\"Coba lagi\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_CANCELLABLE"));
    }

    @Test
    void midtransOrder_staysCreated_untilPaid() throws Exception {
        mockMvc.perform(post(ORDERS).header(AUTHORIZATION, bearer(registerCustomer()))
                .contentType(APPLICATION_JSON).content(orderJson("MIDTRANS", "1.5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));
    }

    @Test
    void otherCustomer_cannotSeeMyOrder_gets404() throws Exception {
        MvcResult created = mockMvc.perform(post(ORDERS).header(AUTHORIZATION, bearer(registerCustomer()))
                .contentType(APPLICATION_JSON).content(orderJson("CASH", "1.5")))
                .andReturn();
        String code = read(created, "$.code");

        mockMvc.perform(get(ORDERS + "/" + code).header(AUTHORIZATION, bearer(registerCustomer())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void overweightForMotor_returns400() throws Exception {
        mockMvc.perform(post(ORDERS).header(AUTHORIZATION, bearer(registerCustomer()))
                .contentType(APPLICATION_JSON).content(orderJson("CASH", "25")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OVERWEIGHT"));
    }

    // private static String orderJson(String paymentMethod, String weightKg) {
    // return """
    // {"serviceType":"INSTANT","vehicleType":"MOTOR","itemCategory":"DOCUMENT",
    // "itemDesc":"Berkas kontrak","weightKg":%s,
    // "pickup":{"address":"Titik A","lat":-6.90,"lng":107.60},
    // "dropoff":{"address":"Titik B","lat":-6.91,"lng":107.60},
    // "recipientName":"Budi","recipientPhone":"081298765432","notes":"Titip
    // resepsionis",
    // "paymentMethod":"%s"}
    // """.formatted(weightKg, paymentMethod);
    // }
}