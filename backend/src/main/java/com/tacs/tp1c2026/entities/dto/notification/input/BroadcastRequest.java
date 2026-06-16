package com.tacs.tp1c2026.entities.dto.notification.input;

import com.tacs.tp1c2026.entities.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Body para un broadcast del admin a todos los users. {@code type} elige la semántica del aviso
 * (típicamente {@link NotificationType#GLOBAL_ANNOUNCEMENT}). {@code referenceId}/{@code link}
 * opcionales (deep-link a un recurso). {@code validUntil} opcional (caducidad; null = no caduca).
 */
public record BroadcastRequest(
    @NotNull NotificationType type,
    @NotBlank @Size(max = 2000) String message,
    String referenceId,
    String link,
    LocalDateTime validUntil
) {}
