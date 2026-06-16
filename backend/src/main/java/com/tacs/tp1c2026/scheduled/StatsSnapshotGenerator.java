package com.tacs.tp1c2026.scheduled;

import com.tacs.tp1c2026.services.StatsSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Genera el snapshot diario de stats (Capa 2 del dashboard admin). Dos triggers:
 * <ul>
 *   <li><b>Cron 00:05 todos los días</b>: calcula el snapshot del día previo. {@code @Retryable}
 *       en {@link StatsSnapshotService#computeForDate} cubre fallas transitorias de Mongo.</li>
 *   <li><b>Backfill on-startup</b>: al levantarse el server, async, completa los snapshots
 *       faltantes (alineado con el TTL = 30 días). Cubre el caso "cron caído por días" o
 *       "sistema recién desplegado".</li>
 * </ul>
 */
@Component
public class StatsSnapshotGenerator {

  private static final Logger log = LoggerFactory.getLogger(StatsSnapshotGenerator.class);

  private final StatsSnapshotService statsSnapshotService;

  public StatsSnapshotGenerator(StatsSnapshotService statsSnapshotService) {
    this.statsSnapshotService = statsSnapshotService;
  }

  @Scheduled(cron = "${app.scheduled.statsSnapshot.cron:0 5 0 * * *}")
  public void generateYesterdaySnapshot() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    try {
      statsSnapshotService.computeForDate(yesterday);
      log.info("StatsSnapshotGenerator: snapshot generado para {}", yesterday);
    } catch (Exception e) {
      log.error("StatsSnapshotGenerator: falló el snapshot de {} tras los retries: {}", yesterday, e.getMessage());
    }
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void backfillOnStartup() {
    try {
      int generated = statsSnapshotService.backfillMissing();
      if (generated > 0) {
        log.info("StatsSnapshotGenerator: backfill on-startup generó {} snapshot(s) faltante(s)", generated);
      }
    } catch (Exception e) {
      log.error("StatsSnapshotGenerator: backfill on-startup falló: {}", e.getMessage());
    }
  }
}
