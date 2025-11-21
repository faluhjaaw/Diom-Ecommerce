package com.dic1.projettrans.cartservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final InternalTokenFilter internalTokenFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, InternalTokenFilter internalTokenFilter) {
        this.jwtFilter = jwtFilter;
        this.internalTokenFilter = internalTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Endpoints internes - DOIT être avant /api/carts/**
                        .requestMatchers("/internal/**", "/api/carts/internal/**").permitAll()

                        // Panier nécessite authentification
                        .requestMatchers("/api/carts/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}