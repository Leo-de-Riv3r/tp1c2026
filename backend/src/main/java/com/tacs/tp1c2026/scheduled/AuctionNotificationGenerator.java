package com.tacs.tp1c2026.scheduled;

import com.tacs.tp1c2026.services.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionNotificationGenerator {

  private final NotificationService notificationService;

  public AuctionNotificationGenerator(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /**
   * Barre las notificaciones agendadas ya vencidas y las entrega. El intervalo es configurable
   * vía {@code app.scheduled.alert.delayMinutes} (default 5 min) — más fino que el barrido
   * horario anterior, para mejor precisión de los avisos de "subasta por cerrar".
   */
  @Scheduled(fixedDelayString = "#{${app.scheduled.alert.delayMinutes:5} * 60000}")
  public void notifyInterestedUsers() {
    notificationService.checkScheduledNotifications();
  }
}
