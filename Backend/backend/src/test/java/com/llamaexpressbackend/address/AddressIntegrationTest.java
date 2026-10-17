package com.llamaexpressbackend.address;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressIntegrationTest extends AbstractIntegrationTest {

    private static final String ADDRESSES = "/api/v1/addresses";

    @Test
    void customer_canManageOwnAddresses_othersGet404() throws Exception {
        String token = registerCustomer();

        MvcResult created = mockMvc.perform(post(ADDRESSES).header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content(addressJson("Rumah")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Rumah"))
                .andReturn();
        Number id = read(created, "$.id");

        mockMvc.perform(get(ADDRESSES).header(AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put(ADDRESSES + "/" + id).header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content(addressJson("Kantor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Kantor"));

        String stranger = registerCustomer();
        mockMvc.perform(put(ADDRESSES + "/" + id).header(AUTHORIZATION, bearer(stranger))
                .contentType(APPLICATION_JSON).content(addressJson("Curian")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(ADDRESSES + "/" + id).header(AUTHORIZATION, bearer(stranger)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(ADDRESSES + "/" + id).header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(ADDRESSES).header(AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void maximumTenAddresses() throws Exception {
        String token = registerCustomer();
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(ADDRESSES).header(AUTHORIZATION, bearer(token))
                    .contentType(APPLICATION_JSON).content(addressJson("Alamat " + i)))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post(ADDRESSES).header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content(addressJson("Ke-11")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADDRESS_LIMIT"));
    }

    @Test
    void courier_cannotUseAddressBook() throws Exception {
        mockMvc.perform(get(ADDRESSES).header(AUTHORIZATION, bearer(registerCourier("MOTOR"))))
                .andExpect(status().isForbidden());
    }

    private static String addressJson(String label) {
        return """
                {"label":"%s","address":"Jl. Dago No. 1, Bandung","lat":-6.885,"lng":107.613,"notes":"Pagar hijau"}
                """.formatted(label);
    }
}