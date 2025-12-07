package com.merigaumata.auth.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends BusinessException {
    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(
                message,
                HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED",
                java.util.Map.of("retryAfter", retryAfterSeconds)
        );
    }
}