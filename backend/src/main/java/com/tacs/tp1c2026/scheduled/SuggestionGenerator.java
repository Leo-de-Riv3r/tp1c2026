 package com.tacs.tp1c2026.scheduled;

 import com.tacs.tp1c2026.services.ProfileService;
 import org.springframework.scheduling.annotation.Scheduled;
 import org.springframework.stereotype.Component;


 @Component
 public class SuggestionGenerator {

     private final ProfileService perfilService;

     public SuggestionGenerator(ProfileService profileService) {
         this.perfilService = profileService;
     }

     /**
      * Tarea programada que ejecuta el cálculo de sugerencias de intercambio para todos
      * los usuarios del sistema. Se ejecuta según el cron configurado en
      * {@code app.scheduled.example.cron} (por defecto cada hora en punto).
      */
     @Scheduled(cron = "${app.scheduled.profiles.cron:0 0 * * * *}")
     public void generarSugerencias() {
         perfilService.updateSuggestionsForUsers();
     }

 }
