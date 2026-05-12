package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.notification.Notification;
import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository extends Repository<Notification, String> {
  Page<Notification> findByReceiverIdAndStatus(String userId, NotificationStatus status, Pageable pageable);
}
