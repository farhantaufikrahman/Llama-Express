package com.llamaexpressbackend.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    /**
     * INCR + PEXPIRE dalam satu langkah atomik; kembalikan [jumlah hit, sisa TTL
     * ms]
     */
    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return {current, redis.call('PTTL', KEYS[1])}
            """, List.class);

    private final StringRedisTemplate redis;

    public Decision hit(String key, int limit, Duration window) {
        List<?> result = redis.execute(SCRIPT, List.of(key), String.valueOf(window.toMillis()));
        long count = ((Number) result.get(0)).longValue();
        long ttlMs = Math.max(((Number) result.get(1)).longValue(), 0);
        return new Decision(count <= limit, limit, Math.max(limit - count, 0), ttlMs);
    }

    public record Decision(boolean allowed, int limit, long remaining, long resetAfterMs) {
        public long retryAfterSeconds() {
            return Math.max(1, (resetAfterMs + 999) / 1000);
        }
    }
}