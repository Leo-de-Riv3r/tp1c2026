package com.tacs.tp1c2026.entities.dto.common;

import java.time.LocalDateTime;

/**
 * Cuerpo de respuesta de error estándar para todos los errores de la API.
 * Devuelto por GlobalExceptionAdvice ante cualquier excepción manejada o inesperada.
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