package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresRole;
import com.tacs.tp1c2026.entities.dto.notification.input.BroadcastRequest;
import com.tacs.tp1c2026.entities.notification.GlobalNotification;
import com.tacs.tp1c2026.services.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Broadcast del admin a todos los users con role=USER. Persiste un {@link GlobalNotification}
 * stateless y agrega una referencia en la cola de cada user mediante {@code updateMulti}
 * server-side (sin loop en memoria).
 */
@RestController
@RequestMapping("/admin/broadcast")
public class AdminBroadcastController {

  private final NotificationService notificationService;

  public AdminBroadcastController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @PostMapping
  @RequiresRole("ADMIN")
  public ResponseEntity<GlobalNotification> broadcast(@Valid @RequestBody BroadcastRequest request) {
    GlobalNotification created = notificationService.createGlobalNotification(
        request.type(), request.message(), request.referenceId(), request.link(), request.validUntil());
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
