package com.llamaexpressbackend.tracking;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/** "Middleware" untuk setiap frame STOMP yang masuk dari client */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final TrackingAccessService accessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand(); // dibaca SEKALI, lalu dipakai lewat variabel lokal
        if (command == null) {
            return message; // frame heartbeat tidak punya command
        }
        switch (command) {
            case CONNECT -> accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
            case SUBSCRIBE -> {
                Authentication auth = (Authentication) accessor.getUser();
                if (!accessService.canSubscribe(auth, accessor.getDestination())) {
                    throw new AccessDeniedException("Tidak boleh berlangganan " + accessor.getDestination());
                }
            }
            case SEND -> throw new AccessDeniedException("Client tidak boleh mengirim pesan lewat WebSocket");
            default -> {
            }
        }
        return message;
    }

    private Authentication authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("Token wajib dikirim di header CONNECT");
        }
        try {
            Jwt jwt = jwtDecoder.decode(header.substring(7)); // validator yang SAMA dengan REST: tanda tangan, exp, iss
            return jwtAuthenticationConverter.convert(jwt);
        } catch (JwtException e) {
            throw new AccessDeniedException("Token tidak valid: " + e.getMessage());
        }
    }
}