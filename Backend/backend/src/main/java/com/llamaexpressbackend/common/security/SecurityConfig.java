package com.llamaexpressbackend.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private static final String[] PUBLIC_PATHS = {
                        "/api/v1/auth/**",
                        "/actuator/health",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                        "/error"
        };

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PUBLIC_PATHS).permitAll()
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/v1/courier/**").hasRole("COURIER")
                                                .requestMatchers("/api/v1/orders/**").hasRole("CUSTOMER")
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(o -> o.jwt(
                                                jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
                return http.build();
        }
}