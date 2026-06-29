package com.tacs.tp1c2026.scheduled;

import com.tacs.tp1c2026.services.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class SuggestionGenerator {

    private static final Logger log = LoggerFactory.getLogger(SuggestionGenerator.class);

    private final ProfileService profileService;

    public SuggestionGenerator(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Al arranque: siembra los grupos de perfil si faltan (caso cloud, donde nada los inicializa)
     * y corre una generación inicial, así las sugerencias quedan disponibles sin esperar a la
     * próxima hora en punto. Async para no bloquear el arranque del server.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void seedGroupsAndGenerateOnStartup() {
        try {
            profileService.seedGroupsIfEmpty();
            profileService.updateSuggestionsForUsers();
            log.info("SuggestionGenerator: grupos sembrados (si faltaban) y sugerencias generadas al arranque");
        } catch (Exception e) {
            log.error("SuggestionGenerator: seed/generación al arranque falló: {}", e.getMessage());
        }
    }

    /**
     * Tarea programada que ejecuta el cálculo de sugerencias de intercambio para todos
     * los usuarios del sistema. Se ejecuta según el cron configurado en
     * {@code app.scheduled.profiles.cron} (por defecto cada 10 minutos).
     */
    @Scheduled(cron = "${app.scheduled.profiles.cron:0 */10 * * * *}")
    public void generateSuggestions() {
        profileService.updateSuggestionsForUsers();
    }

}
