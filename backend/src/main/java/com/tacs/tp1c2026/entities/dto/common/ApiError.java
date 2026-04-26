package com.tacs.tp1c2026.entities.dto.common;

import java.time.LocalDateTime;

/**
 * Standard error response body for all API errors
 * Returned by GlobalExceptionAdvice for any handled or unexpected exception
 */
public record ApiError(
    int status,
    String error,
    String message,
    LocalDateTime timestamp
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, LocalDateTime.now());
    }
}