package com.tacs.tp1c2026.entities.notification;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Notificación global stateless: se guarda una sola vez y se distribuye por
 * referencia a la cola de cada user. No tiene estado de lectura (eso vive en el
 * {@link UserNotification} que la referencia). Caduca sola vía {@code validUntil}.
 */
@Document(collection = "global_notifications")
@TypeAlias("globalNotification")
@Getter
@Builder
public class GlobalNotification {

  @Id
  private String id;

  private NotificationData data;

  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  /** Opcional: si está seteada, deja de mostrarse después de esta fecha. */
  private LocalDateTime validUntil;

  public boolean isActive(LocalDateTime now) {
    return validUntil == null || validUntil.isAfter(now);
  }
}
