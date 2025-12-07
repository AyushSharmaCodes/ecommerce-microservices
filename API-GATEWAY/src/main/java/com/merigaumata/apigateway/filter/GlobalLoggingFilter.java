package com.merigaumata.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header("X-Request-Id", requestId)
                .build();

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;

            String logMessage = String.format(
                    "{\"timestamp\":\"%s\",\"method\":\"%s\",\"path\":\"%s\",\"status\":%d," +
                            "\"duration_ms\":%d,\"remote_address\":\"%s\",\"correlation_id\":\"%s\"}",
                    java.time.Instant.now(),
                    request.getMethod(),
                    request.getPath(),
                    response.getStatusCode() != null ? response.getStatusCode().value() : 0,
                    duration,
                    request.getRemoteAddress(),
                    request.getHeaders().getFirst("X-Correlation-Id")
            );

            log.info(logMessage);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}