package com.llamaexpressbackend.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class OrderCodeGenerator {

    /** Tanpa 0/O dan 1/I supaya tidak tertukar saat dibacakan lewat telepon */
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd")
            .withZone(ZoneId.of("Asia/Jakarta"));
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Clock clock;

    /** Contoh: LX-261016-7KQ2MZ */
    public String orderCode() {
        StringBuilder sb = new StringBuilder("LX-").append(DATE.format(clock.instant())).append('-');
        for (int i = 0; i < 6; i++)
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        return sb.toString();
    }

    public String trackingToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String verificationCode() {
        return String.format("%04d", RANDOM.nextInt(10_000));
    }
}