package com.llamaexpressbackend.delivery;

import com.llamaexpressbackend.common.error.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Batasi tebakan kode verifikasi: 5× salah → dikunci 15 menit */
@Component
@RequiredArgsConstructor
public class VerificationAttemptGuard {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;

    public void ensureNotLocked(String orderCode) {
        String attempts = redis.opsForValue().get(key(orderCode));
        if (attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("TOO_MANY_ATTEMPTS",
                    "Terlalu banyak percobaan kode salah. Coba lagi dalam 15 menit");
        }
    }

    public void recordFailure(String orderCode) {
        Long attempts = redis.opsForValue().increment(key(orderCode)); // INCR atomik
        if (attempts != null && attempts == 1) {
            redis.expire(key(orderCode), LOCK_WINDOW); // jendela mulai dari kesalahan pertama
        }
    }

    public void reset(String orderCode) {
        redis.delete(key(orderCode));
    }

    private static String key(String orderCode) {
        return "verify:attempts:" + orderCode;
    }
}