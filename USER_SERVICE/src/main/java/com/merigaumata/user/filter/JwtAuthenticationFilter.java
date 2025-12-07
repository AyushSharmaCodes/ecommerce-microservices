package com.merigaumata.user.filter;

import com.merigaumata.user.service.JwtValidationService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidationService jwtValidationService;

    public JwtAuthenticationFilter(JwtValidationService jwtValidationService) {
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // ===========================================================
        // OPTION 1: JWT Token Authentication (from Gateway or Service)
        // ===========================================================
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Validate JWT token
                JWTClaimsSet claims = jwtValidationService.validateToken(token);

                String userId = claims.getSubject();
                List<String> roles = (List<String>) claims.getClaim("roles");

                log.debug("Authenticated via JWT: userId={}, roles={}", userId, roles);

                // Create Spring Security authentication
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                // Don't throw - let Spring Security handle unauthorized
            }
        }

        // ===========================================================
        // OPTION 2: Header-Based Authentication (from Gateway only)
        // ===========================================================
        else if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String userId = request.getHeader("X-User-Id");
            String rolesHeader = request.getHeader("X-User-Roles");

            if (userId != null && rolesHeader != null) {
                log.debug("Authenticated via headers: userId={}, roles={}", userId, rolesHeader);

                List<SimpleGrantedAuthority> authorities =
                        Arrays.stream(rolesHeader.split(","))
                                .map(String::trim)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}