package com.llamaexpressbackend.dispatch;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Jumlah putaran dispatch per order — disimpan di Redis, BUKAN di baris orders,
 * supaya dispatcher tidak menaikkan @Version order dan bentrok dengan aksi
 * customer.
 */
@Component
@RequiredArgsConstructor
public class DispatchAttemptCounter {

    private static final Duration TTL = Duration.ofDays(1);

    private final StringRedisTemplate redis;

    public int current(String orderCode) {
        String value = redis.opsForValue().get(key(orderCode));
        return (value == null) ? 0 : Integer.parseInt(value);
    }

    public int increment(String orderCode) {
        Long attempts = redis.opsForValue().increment(key(orderCode)); // INCR atomik
        redis.expire(key(orderCode), TTL);
        return attempts == null ? 0 : attempts.intValue();
    }

    public void clear(String orderCode) {
        redis.delete(key(orderCode));
    }

    private static String key(String orderCode) {
        return "dispatch:attempts:" + orderCode;
    }
}