package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user attempts an action they are not allowed to perform
 * Maps to HTTP 403 Forbidden
 * Example: a user trying to accept a proposal that belongs to another user
 */
public class ForbiddenException extends CustomException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}