 package com.tacs.tp1c2026.scheduled;

 import org.springframework.scheduling.annotation.Scheduled;
 import org.springframework.stereotype.Component;

 import com.tacs.tp1c2026.services.AlertService;

 @Component
 public class SubastaAlert {

     private final AlertService alertService;

     public SubastaAlert(AlertService alertService) {
         this.alertService = alertService;
     }

     /**
      * Tarea programada que genera alertas para los usuarios interesados en subastas próximas
      * a cerrar. Se ejecuta periódicamente con el retardo configurado en
      * {@code app.scheduled.alert.delayMinutes} (por defecto 60 minutos).
      */
     @Scheduled(fixedDelayString = "#{${app.scheduled.alert.delayMinutes:60} * 60000}")
     public void alertarUsuarios() {
         alertService.alertarSubastasProximas();
     }
 }
