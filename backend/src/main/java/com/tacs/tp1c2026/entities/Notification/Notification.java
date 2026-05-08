package com.tacs.tp1c2026.entities.Notification;

import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import java.time.LocalDateTime;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "notifications")
@Builder
public class Notification {
  @Id
  private String id;

  private String receiverId;

  private NotificationType type;

  @Builder.Default
  private NotificationStatus status = NotificationStatus.UNREAD;

  private String referenceId;
  @Builder.Default
  private LocalDateTime creationDate = LocalDateTime.now();

  public void validateOwner(String userId) {
    if (!this.receiverId.equals(userId)) {
      throw new ForbiddenException("User is not the owner of this notification");
    }
  }

  public void setAsRead() {
    this.status = NotificationStatus.READ;
  }
}
