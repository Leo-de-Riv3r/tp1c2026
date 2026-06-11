package com.tacs.tp1c2026.entities.notification;

import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@TypeAlias("notification")
@Builder
@Getter
public class Notification {
  @Id
  private String id;

  private String receiverId;

  private NotificationType type;

  @Builder.Default
  private NotificationStatus status = NotificationStatus.UNREAD;

  private String referenceId;

  private String message;

  @Builder.Default
  private LocalDateTime creationDate = LocalDateTime.now();

  public void validateOwner(String userId) {
    if (!this.receiverId.equals(userId)) {
      throw new ForbiddenException("You don't have permission to access this notification");
    }
  }

  public void setAsRead() {
    if(this.isRead()) {
      throw new ConflictException("Notification already read");
    }
    this.status = NotificationStatus.READ;
  }

  public boolean isRead() {
    return this.status == NotificationStatus.READ;
  }
}
