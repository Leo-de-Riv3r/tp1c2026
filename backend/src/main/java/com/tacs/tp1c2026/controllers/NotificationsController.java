package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresRole;
import com.tacs.tp1c2026.entities.dto.notification.input.CreateGlobalNotificationRequest;
import com.tacs.tp1c2026.entities.notification.GlobalNotification;
import com.tacs.tp1c2026.services.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationsController {

  private final NotificationService notificationService;

  public NotificationsController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /**
   * Crea un anuncio global (stateless) y lo distribuye a la cola de todos los usuarios.
   * Solo ADMIN.
   */
  @PostMapping("/global")
  @RequiresRole("ADMIN")
  public ResponseEntity<GlobalNotification> createGlobal(
      @Valid @RequestBody CreateGlobalNotificationRequest request) {
    GlobalNotification created = notificationService.createGlobalNotification(
        request.type(), request.message(), request.referenceId(), request.link(), request.validUntil());
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
