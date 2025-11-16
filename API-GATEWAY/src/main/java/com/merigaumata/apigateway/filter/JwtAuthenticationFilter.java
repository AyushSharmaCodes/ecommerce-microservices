package com.merigaumata.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final ReactiveJwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${jwt.issuer:auth-service}")
    private String expectedIssuer;

    @Value("${jwt.audience:api-gateway}")
    private String expectedAudience;

    private static final List<String> WHITELIST_PATTERNS = Arrays.asList(
            "/api/auth/login",
            "/oauth2/**",
            "/.well-known/**",
            "/actuator/health",
            "/actuator/info"
    );

    public JwtAuthenticationFilter(ReactiveJwtDecoder jwtDecoder, ObjectMapper objectMapper) {
        this.jwtDecoder = jwtDecoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Check if path is whitelisted
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                    "MISSING_TOKEN", "Authorization header missing or invalid");
        }

        String token = authHeader.substring(7);

        // Validate JWT
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // Validate issuer
                    if (!expectedIssuer.equals(jwt.getIssuer().toString())) {
                        return sendErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                                "INVALID_ISSUER", "Invalid token issuer");
                    }

                    // Validate audience
                    List<String> audiences = jwt.getAudience();
                    if (audiences == null || !audiences.contains(expectedAudience)) {
                        return sendErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                                "INVALID_AUDIENCE", "Invalid token audience");
                    }

                    // Add user context headers
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(builder -> {
                                builder.header("X-User-Id", jwt.getSubject());

                                // Extract roles
                                Object rolesObj = jwt.getClaims().get("roles");
                                if (rolesObj instanceof List) {
                                    String roles = String.join(",", (List<String>) rolesObj);
                                    builder.header("X-User-Roles", roles);
                                }
                            })
                            .build();

                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(JwtException.class, ex -> {
                    if (ex.getMessage().contains("expired")) {
                        return sendErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                                "TOKEN_EXPIRED", "Access token expired. Use refresh endpoint.");
                    }
                    return sendErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                            "INVALID_TOKEN", "Invalid or malformed JWT token");
                });
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, HttpStatus status,
                                         String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("code", code);
        errorBody.put("message", message);
        errorBody.put("timestamp", String.valueOf(System.currentTimeMillis()));

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorBody);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        } catch (Exception e) {
            return exchange.getResponse().setComplete();
        }
    }
}