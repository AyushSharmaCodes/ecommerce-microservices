package com.merigaumata.user.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String resource, String field, String value) {
        super(
                String.format("%s already exists with %s: %s", resource, field, value),
                HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                java.util.Map.of("resource", resource, "field", field, "value", value)
        );
    }
}