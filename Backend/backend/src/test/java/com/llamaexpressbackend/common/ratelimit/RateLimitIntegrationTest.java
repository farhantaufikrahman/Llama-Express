package com.llamaexpressbackend.common.ratelimit;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String BAD_LOGIN = """
            {"email":"tidak-ada@test.com","password":"salah12345"}
            """;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private RateLimitProperties props;

    @Test
    void ruledEndpoint_returnsRateLimitHeaders() throws Exception {
        int limit = props.rules().get("auth").limit();

        mockMvc.perform(
                post("/api/v1/auth/login").with(from(randomIp())).contentType(APPLICATION_JSON).content(BAD_LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Limit", String.valueOf(limit)))
                .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(limit - 1)));
    }

    @Test
    void exceedingLimit_returns429_onlyForThatIp() throws Exception {
        String ip = randomIp();
        int limit = props.rules().get("auth").limit();
        // Isi penghitung langsung sampai batas — lebih cepat daripada mengirim 1000
        // request
        redis.opsForValue().set(RateLimitFilter.key("auth", ip), String.valueOf(limit), Duration.ofSeconds(30));

        mockMvc.perform(post("/api/v1/auth/login").with(from(ip)).contentType(APPLICATION_JSON).content(BAD_LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        mockMvc.perform(
                post("/api/v1/auth/login").with(from(randomIp())).contentType(APPLICATION_JSON).content(BAD_LOGIN))
                .andExpect(status().isUnauthorized()); // IP lain tidak terpengaruh
    }

    @Test
    void endpointWithoutRule_isNotLimited() throws Exception {
        mockMvc.perform(get("/actuator/health").with(from(randomIp())))
                .andExpect(header().doesNotExist("X-RateLimit-Limit"));
    }

    /**
     * MockMvc default selalu 127.0.0.1 → setiap test memakai IP acak supaya tidak
     * saling memengaruhi
     */
    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private static String randomIp() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return "10." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(1, 255);
    }
}