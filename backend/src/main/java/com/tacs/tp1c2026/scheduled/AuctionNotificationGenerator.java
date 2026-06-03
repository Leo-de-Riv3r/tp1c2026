package com.tacs.tp1c2026.scheduled;// package com.tacs.tp1c2026.scheduled;

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
      * Tarea programada que genera alertas para los usuarios interesados en subastas próximas
      * a cerrar. Se ejecuta periódicamente con el retardo configurado en
      * {@code app.scheduled.alert.delayMinutes} (por defecto 60 minutos).
      */
     @Scheduled(fixedDelayString = "#{${app.scheduled.alert.delayMinutes:60} * 60000}")
     public void notifyInterestedUsers() {
         notificationService.checkScheduledNotifications();
     }
 }
