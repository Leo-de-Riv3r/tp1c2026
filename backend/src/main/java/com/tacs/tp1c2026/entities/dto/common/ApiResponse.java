package com.tacs.tp1c2026.entities.dto.common;

import java.time.LocalDateTime;

/**
 * Envelope genérico para responses POST/PUT que reemplaza los {@code Map.of(...)} sueltos que devolvían los controllers
 * Usar {@link #of(String, Object)} para 201s que devuelven un recurso completo o un id de un side effect (ej. {@code acceptProposal} devuelve el {@code exchangeId} creado)
 * Usar {@link #of(String)} para acciones donde solo se quiere transportar el mensaje ({@code data} queda en {@code null})
 */
public record ApiResponse<T>(
    LocalDateTime timestamp,
    String message,
    T data
) {
    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(LocalDateTime.now(), message, data);
    }

    public static ApiResponse<Void> of(String message) {
        return new ApiResponse<>(LocalDateTime.now(), message, null);
    }
}
