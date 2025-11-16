package com.merigaumata.apigateway.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping(value = "/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> fallback(@PathVariable String service) {
        Map<String, Object> body = Map.of(
                "service", service,
                "status", "unavailable",
                "message", "The service is currently unavailable. This is a gateway fallback."
        );
        return Mono.just(body);
    }

    @PostMapping(value = "/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> fallback2(@PathVariable String service) {
        Map<String, Object> body = Map.of(
                "service", service,
                "status", "unavailable",
                "message", "The service is currently unavailable. This is a gateway fallback."
        );
        return Mono.just(body);
    }
}