package com.merigaumata.apigateway.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    // Removed custom LettuceConnectionFactory bean to avoid conflicts with Spring Boot auto-configuration.
    // Spring Boot will auto-configure the reactive Redis connection factory based on spring.data.redis.* properties.
}