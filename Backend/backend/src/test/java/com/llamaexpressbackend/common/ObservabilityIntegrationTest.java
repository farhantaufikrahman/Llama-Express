package com.llamaexpressbackend.common;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ObservabilityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MeterRegistry registry;

    @Test
    void probes_arePublic_andUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void requestId_isGenerated_reused_orReplacedWhenUnsafe() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));

        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "frontend-abc-12345"))
                .andExpect(header().string("X-Request-Id", "frontend-abc-12345"));

        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "jahat\" spasi <script>"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.not("jahat\" spasi <script>")));
    }

    @Test
    void errorBody_carriesSameRequestIdAsHeader() throws Exception {
        MvcResult r = mockMvc
                .perform(get("/api/v1/admin/orders/LX-TIDAK-ADA").header(AUTHORIZATION, bearer(loginAdmin())))
                .andExpect(status().isNotFound())
                .andReturn();
        String bodyId = read(r, "$.requestId");
        assertThat(bodyId).isEqualTo(r.getResponse().getHeader("X-Request-Id"));
    }

    @Test
    void creatingOrder_incrementsBusinessCounters() throws Exception {
        double createdBefore = sum("llama.orders.placed");

        createCashOrderAt(registerCustomer(), -6.10, 106.80);

        assertThat(sum("llama.orders.placed")).isEqualTo(createdBefore + 1);
        assertThat(registry.find("llama.orders.transitions").counters()).isNotEmpty();
    }

    @Test
    void operationalGauges_areRegistered() {
        assertThat(registry.find("llama.outbox.pending").gauge()).isNotNull();
        assertThat(registry.find("llama.couriers.online").gauge()).isNotNull();
        assertThat(registry.find("llama.orders.searching").gauge().value()).isGreaterThanOrEqualTo(0);
    }

    /**
     * Total semua kombinasi label — tes lain di context yang sama juga menambah
     * angka, jadi kita ukur selisihnya
     */
    private double sum(String name) {
        return registry.find(name).counters().stream().mapToDouble(Counter::count).sum();
    }
}