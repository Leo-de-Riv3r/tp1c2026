package com.tacs.tp1c2026.entities.notification;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Notificación agendada para dispararse en el futuro (ej. "subasta por cerrar" a varios
 * lead-times antes del cierre). Acumula los ids de usuarios interesados; cuando vence, el
 * {@code NotificationService} la entrega a cada uno como una {@link UserNotification} propia.
 * Vive en Mongo, así que el cron sobrevive a restarts.
 */
@Document(collection = "scheduled_notifications")
@TypeAlias("scheduledNotification")
@Getter
@Builder
public class ScheduledNotification {

  @Id
  private String id;

  private NotificationData notificationData;

  private LocalDateTime scheduledTime;

  @Builder.Default
  private List<String> userIds = new ArrayList<>();

  public boolean isDue(LocalDateTime now) {
    return scheduledTime.isBefore(now);
  }

  public boolean hasReferenceId(String referenceId) {
    return Objects.equals(this.notificationData.getReferenceId(), referenceId);
  }

  public void addUser(String userId) {
    if (!this.userIds.contains(userId)) {
      this.userIds.add(userId);
    }
  }
}
