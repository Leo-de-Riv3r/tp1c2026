package com.tacs.tp1c2026.scheduled;

import com.tacs.tp1c2026.services.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job programado que cierra subastas vencidas. Cada vez que se ejecuta busca las
 * subastas activas con `closeDate` en el pasado y las adjudica (si tienen mejor oferta)
 * o las cancela liberando el compromise (si no recibieron ninguna).
 *
 * Frecuencia configurable vía {@code app.scheduled.auctionCloser.cron}.
 * Por defecto: cada 5 minutos.
 */
@Component
public class AuctionCloser {

  private static final Logger log = LoggerFactory.getLogger(AuctionCloser.class);

  private final AuctionService auctionService;

  public AuctionCloser(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Scheduled(cron = "${app.scheduled.auctionCloser.cron:0 */5 * * * *}")
  public void closeExpiredAuctions() {
    int closed = auctionService.closeAllExpiredAuctions();
    if (closed > 0) {
      log.info("AuctionCloser: closed {} expired auction(s)", closed);
    }
  }
}
