package com.tacs.tp1c2026.entities.notification;

import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.ForbiddenException;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@Builder
@Getter
public class Notification {
    @Builder
    @Getter
    public static class NotificationData {

        private NotificationType type;

        private String referenceId;

        private String message;

    }

    @Id
    private String id;

    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    private NotificationData data;

    @Builder.Default
    private LocalDateTime creationDate = LocalDateTime.now();

    public void setAsRead() {
        if (this.isRead()) {
            throw new ConflictException("La notificación ya se encuentra leída");
        }
        this.status = NotificationStatus.READ;
    }

    public boolean isRead() {
        return this.status == NotificationStatus.READ;
    }
}
