package com.llamaexpressbackend.common.security;

import com.llamaexpressbackend.common.config.CorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private static final String[] PUBLIC_PATHS = {
                        "/api/v1/auth/**",
                        "/api/v1/track/**", // link lacak publik
                        "/ws/**", // handshake WebSocket — auth dicek di frame STOMP CONNECT
                        "/api/v1/payments/midtrans/notification", // dipanggil server Midtrans; keamanan lewat signature
                        "/actuator/health",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                        "/error"
        };

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PUBLIC_PATHS).permitAll()
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/v1/courier/**").hasRole("COURIER")
                                                .requestMatchers("/api/v1/orders/**").hasRole("CUSTOMER")
                                                .requestMatchers("/api/v1/addresses/**").hasRole("CUSTOMER")
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(o -> o.jwt(
                                                jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
                return http.build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource(CorsProperties props) {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(props.allowedOrigins());
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                config.setMaxAge(3600L);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}