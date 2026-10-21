package com.llamaexpressbackend.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Setiap request mendapat ID unik: dipakai di semua baris log, dikembalikan di
 * header & body error.
 * Kalau frontend/Nginx sudah mengirim X-Request-Id yang valid, ID itu dipakai
 * ulang → bisa dilacak end-to-end.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // paling awal, supaya log dari filter lain (security, rate limit) ikut punya ID
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    /**
     * Hanya karakter aman: mencegah log injection (mis. ID berisi baris baru palsu)
     */
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = (incoming != null && SAFE_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY); // WAJIB: thread Tomcat dipakai ulang — tanpa ini, ID bocor ke request
                                 // berikutnya
        }
    }
}