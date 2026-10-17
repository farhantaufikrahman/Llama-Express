package com.llamaexpressbackend.courier.earnings;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourierEarningsIntegrationTest extends AbstractIntegrationTest {

    private static final String EARNINGS = "/api/v1/courier/earnings";

    @Test
    void newCourier_hasZeroEarnings_withEveryDayListed() throws Exception {
        mockMvc.perform(get(EARNINGS + "?from=2026-10-01&to=2026-10-07")
                .header(AUTHORIZATION, bearer(registerCourier("MOTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveredCount").value(0))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.netPayout").value(0));
    }

    @Test
    void invalidRanges_areRejected() throws Exception {
        String token = registerCourier("MOTOR");
        mockMvc.perform(get(EARNINGS + "?from=2026-10-10&to=2026-10-01").header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RANGE"));
        mockMvc.perform(get(EARNINGS + "?from=2026-01-01&to=2026-12-31").header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RANGE_TOO_LONG"));
    }
}