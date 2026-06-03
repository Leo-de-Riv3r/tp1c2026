package com.tacs.tp1c2026.entities.dto.user.output;

import com.tacs.tp1c2026.entities.notification.Notification;

public record NotificationDto(String id, String message, boolean read, String type, String referenceId) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getData().getMessage(), n.isRead(), n.getData().getType().name(), n.getData().getReferenceId());
    }
}
