package com.tacs.tp1c2026.scheduled;

import com.tacs.tp1c2026.services.UpcomingMatchesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el refresh del cache de próximos partidos (bonus). Dos triggers, mismo patrón que
 * {@link StatsSnapshotGenerator}:
 * <ul>
 *   <li><b>Cron diario</b> (04:30 por defecto): 1 call/día a API-Football, dentro del free tier.</li>
 *   <li><b>On-startup</b> (async): puebla el cache al levantar el server, así el Home tiene data
 *       sin esperar al primer cron.</li>
 * </ul>
 * La resiliencia (key ausente, API caída, respuesta vacía) vive en el service.
 */
@Component
public class UpcomingMatchesGenerator {

  private static final Logger log = LoggerFactory.getLogger(UpcomingMatchesGenerator.class);

  private final UpcomingMatchesService service;

  public UpcomingMatchesGenerator(UpcomingMatchesService service) {
    this.service = service;
  }

  @Scheduled(cron = "${app.scheduled.upcomingMatches.cron:0 30 4 * * *}")
  public void refreshDaily() {
    try {
      service.refreshFromApi();
    } catch (Exception e) {
      log.error("UpcomingMatchesGenerator: refresh diario falló: {}", e.getMessage());
    }
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void refreshOnStartup() {
    try {
      service.refreshFromApi();
    } catch (Exception e) {
      log.error("UpcomingMatchesGenerator: refresh on-startup falló: {}", e.getMessage());
    }
  }
}
