package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the request lacks valid authentication credentials
 * Maps to HTTP 401 Unauthorized
 * Note: in most cases Spring Security handles this before reaching the controller
 */
public class UnauthorizedException extends CustomException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}