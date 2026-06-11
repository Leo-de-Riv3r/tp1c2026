package com.tacs.tp1c2026.entities.dto.user.output;

import com.tacs.tp1c2026.entities.notification.NotificationData;
import com.tacs.tp1c2026.entities.notification.UserNotification;

/**
 * Vista de una notificación para el FE. {@code referenceId}/{@code type} permiten al FE
 * resolver el render y el deep-link. El contenido sale resuelto (inline para propias,
 * desde la global para las referenciadas).
 */
public record NotificationDto(String id, String message, boolean read, String type, String referenceId) {

  public static NotificationDto of(UserNotification notification, NotificationData data) {
    return new NotificationDto(
        notification.getId(),
        data.getMessage(),
        !notification.isUnread(),
        data.getType().name(),
        data.getReferenceId()
    );
  }
}
