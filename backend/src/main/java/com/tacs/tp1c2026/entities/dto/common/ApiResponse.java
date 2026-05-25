package com.tacs.tp1c2026.entities.dto.common;

import java.time.LocalDateTime;

public record ApiResponse(
    String message,
    LocalDateTime timestamp,
    String id
) {
    public static ApiResponse of(String message) {
        return new ApiResponse(message, LocalDateTime.now(), null);
    }

    public static ApiResponse of(String message, String id) {
        return new ApiResponse(message, LocalDateTime.now(), id);
    }
}
