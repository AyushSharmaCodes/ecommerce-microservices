package com.merigaumata.user.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Simple service-to-service authentication using shared secret
 * For production, use JWT-based service tokens instead
 */
@Slf4j
@Component
@Order(1) // Execute before JwtAuthenticationFilter
public class ServiceAuthenticationFilter extends OncePerRequestFilter {

    @Value("${service.secret:}")
    private String serviceSecret;

    // Internal endpoints that require service authentication
    private static final List<String> SERVICE_ENDPOINTS = List.of(
            "/api/users",
            "/api/users/username/"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Check if this is an internal service endpoint
        boolean isServiceEndpoint = SERVICE_ENDPOINTS.stream()
                .anyMatch(endpoint -> {
                    if (endpoint.endsWith("/")) {
                        return path.startsWith(endpoint);
                    }
                    return path.equals(endpoint) && "POST".equals(method);
                });

        if (isServiceEndpoint) {
            String serviceToken = request.getHeader("X-Service-Token");
            String serviceClient = request.getHeader("X-Service-Client");

            // Validate service authentication
            if (serviceToken != null && serviceToken.equals(serviceSecret)) {
                log.debug("Service authenticated: {}", serviceClient);

                // Create service authentication with ROLE_SERVICE
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_SERVICE"));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                serviceClient, null, authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("Invalid service credentials from: {}",
                        request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"Invalid service credentials\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}