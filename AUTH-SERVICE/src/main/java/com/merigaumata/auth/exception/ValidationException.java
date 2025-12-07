package com.merigaumata.auth.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BusinessException {
    public ValidationException(String message, Object details) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", details);
    }
}