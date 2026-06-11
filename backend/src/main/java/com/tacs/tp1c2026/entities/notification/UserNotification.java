package com.tacs.tp1c2026.entities.notification;

import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notificación embebida en el {@code User}. Tagged-union: o trae su contenido inline
 * ({@code data}, propia) o referencia a una {@link GlobalNotification} ({@code globalId}).
 * El estado {@code READ/UNREAD} vive SIEMPRE acá (por usuario), nunca en la global,
 * así marcar leída no afecta a nadie más.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification {

  private String id;

  private NotificationStatus status;

  private LocalDateTime createdAt;

  /** Contenido inline; {@code null} si es una referencia a global. */
  private NotificationData data;

  /** Id de la {@link GlobalNotification} referenciada; {@code null} si es propia. */
  private String globalId;

  private UserNotification(NotificationData data, String globalId) {
    this.id = UUID.randomUUID().toString();
    this.status = NotificationStatus.UNREAD;
    this.createdAt = LocalDateTime.now();
    this.data = data;
    this.globalId = globalId;
  }

  public static UserNotification own(NotificationData data) {
    return new UserNotification(data, null);
  }

  public static UserNotification globalRef(String globalId) {
    return new UserNotification(null, globalId);
  }

  public boolean isGlobal() {
    return globalId != null;
  }

  public boolean isUnread() {
    return status == NotificationStatus.UNREAD;
  }

  public void markRead() {
    this.status = NotificationStatus.READ;
  }

  /** True si es una noti propia, sin leer, que apunta al recurso dado (para dedupe). */
  public boolean isUnreadOwnReferencing(String referenceId) {
    return !isGlobal() && isUnread()
        && data != null && referenceId.equals(data.getReferenceId());
  }
}
