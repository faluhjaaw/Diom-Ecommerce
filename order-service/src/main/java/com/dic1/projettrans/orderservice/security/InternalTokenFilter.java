package com.dic1.projettrans.orderservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    @Value("${internal.token}")
    private String internalToken;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Vérifie seulement les endpoints internes
        if (path.startsWith("/internal") || path.contains("/internal/")) {

            String token = request.getHeader("X-INTERNAL-TOKEN");

            if (token == null || !token.equals(internalToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid internal token");
                return;
            }

            // On crée une "fausse" authentification interne
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            "internal-service",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
