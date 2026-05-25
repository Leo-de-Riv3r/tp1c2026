package com.tacs.tp1c2026.entities.dto.common.output;

import java.time.LocalDateTime;

/**
 * Corrección por devolución de la entrega 2: Es un envelope para responses de creación (HTTP 201)
 * Reemplaza los {@code Map.of(...)} sueltos que devolvían los controllers y le da al cliente el recurso completo (no solo el id) para evitar un round-trip GET inmediato
 * Para acciones que no crean un recurso nuevo (PUT accept/reject/cancel, POST feedback, etc.) se prefiere 204 No Content sin body — no usar este DTO (por ahora, quizás creemos otro acorde para esos casos)
 *
 * @param timestamp momento del response
 * @param message   mensaje human-readable para mostrar en UI (toast, etc.)
 * @param data      el recurso recién creado (DTO completo, no solo el id)
 */
public record CreatedResponseDto<T>(
    LocalDateTime timestamp,
    String message,
    T data
) {
  public static <T> CreatedResponseDto<T> of(String message, T data) {
    return new CreatedResponseDto<>(LocalDateTime.now(), message, data);
  }
}
