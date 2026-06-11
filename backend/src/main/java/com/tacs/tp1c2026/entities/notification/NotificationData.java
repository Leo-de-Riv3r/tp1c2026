package com.tacs.tp1c2026.entities.notification;

import com.tacs.tp1c2026.entities.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Contenido semántico de una notificación (qué pasó), reutilizado tanto por las
 * notificaciones propias embebidas en el {@code User} como por las globales.
 * Solo semántica: la presentación (texto / link / card rica) la resuelve el FE según
 * {@code type}. No guardamos HTML acá a propósito.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationData {

  private NotificationType type;

  private String message;

  /** Recurso al que apunta la noti: subasta, publicación o carta, según el {@code type}. */
  private String referenceId;

  /** Deep-link opcional para el FE. */
  private String link;
}
