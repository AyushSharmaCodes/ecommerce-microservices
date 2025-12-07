package com.merigaumata.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceTokenProvider {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Generate internal service-to-service JWT token
     * These tokens have ROLE_SERVICE and longer TTL
     */
    public String generateServiceToken(String targetService) {
        return jwtTokenProvider.generateAccessToken(
                "auth-service", // Service identifier
                List.of("ROLE_SERVICE"), // Service role
                List.of("service:internal", "user:create", "user:read"), // Service scopes
                targetService // Target service as audience
        );
    }
}