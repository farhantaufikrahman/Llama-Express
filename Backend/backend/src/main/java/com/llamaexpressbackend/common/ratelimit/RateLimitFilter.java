package com.llamaexpressbackend.common.ratelimit;

import com.llamaexpressbackend.common.ratelimit.RateLimitProperties.Rule;
import com.llamaexpressbackend.common.ratelimit.RedisRateLimiter.Decision;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@Order(-99) // tepat SETELAH Spring Security (-100) → respons 429 sudah membawa header CORS
            // untuk frontend
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties props;
    private final RedisRateLimiter limiter;
    private final JsonMapper jsonMapper;

    public static String key(String rule, String clientIp) {
        return "ratelimit:" + rule + ":" + clientIp;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()); // preflight CORS tidak dihitung
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Map.Entry<String, Rule> rule = match(request.getRequestURI());
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        Decision decision;
        try {
            decision = limiter.hit(key(rule.getKey(), request.getRemoteAddr()),
                    rule.getValue().limit(), rule.getValue().window());
        } catch (DataAccessException e) {
            // Fail-open: Redis mati jangan sampai membuat SEMUA orang tidak bisa login
            log.warn("Rate limiter tidak tersedia, request diloloskan: {}", e.getMessage());
            chain.doFilter(request, response);
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        if (!decision.allowed()) {
            reject(request, response, decision);
            return;
        }
        chain.doFilter(request, response);
    }

    private Map.Entry<String, Rule> match(String uri) {
        for (Map.Entry<String, Rule> entry : props.rules().entrySet()) {
            for (String prefix : entry.getValue().paths()) {
                if (uri.startsWith(prefix)) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * Filter berjalan di luar @RestControllerAdvice → ProblemDetail ditulis manual,
     * formatnya sama
     */
    private void reject(HttpServletRequest request, HttpServletResponse response, Decision d) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", "Too Many Requests");
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("detail", "Terlalu banyak permintaan. Coba lagi dalam " + d.retryAfterSeconds() + " detik");
        body.put("instance", request.getRequestURI());
        body.put("code", "RATE_LIMITED");

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(d.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}