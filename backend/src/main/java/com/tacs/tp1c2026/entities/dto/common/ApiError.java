package com.tacs.tp1c2026.entities.dto.common;

import java.time.LocalDateTime;

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