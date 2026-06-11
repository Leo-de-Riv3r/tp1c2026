package com.tacs.tp1c2026.entities.dto.notification.input;

import com.tacs.tp1c2026.entities.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Body para crear un anuncio global. {@code referenceId}/{@code link} opcionales (deep-link),
 * {@code validUntil} opcional (caducidad; null = no caduca).
 */
public record CreateGlobalNotificationRequest(
    @NotNull NotificationType type,
    @NotBlank String message,
    String referenceId,
    String link,
    LocalDateTime validUntil
) {}
