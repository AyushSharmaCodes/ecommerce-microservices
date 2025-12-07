package com.merigaumata.apigateway.filter;

import com.merigaumata.apigateway.service.JwtValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.text.ParseException;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtValidationService jwtValidationService;

    public AuthenticationFilter(JwtValidationService jwtValidationService) {
        super(Config.class);
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            return jwtValidationService.validateToken(token)
                    .flatMap(claims -> {
                        // Add claims to request headers for downstream services
                        ServerHttpRequest modifiedRequest = null;
                        try {
                            modifiedRequest = request.mutate()
                                    .header("X-User-Id", claims.getSubject())
                                    .header("X-User-Roles", String.join(",", claims.getStringListClaim("roles")))
                                    .header("X-User-Scopes", String.join(",", claims.getStringListClaim("scopes")))
                                    .build();
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }

                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    })
                    .onErrorResume(e -> {
                        log.error("JWT validation failed: {}", e.getMessage());
                        return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"error\":\"%s\",\"message\":\"%s\"}",
                status.getReasonPhrase(), message);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }

    public static class Config {
        public Config() {}
    }

}