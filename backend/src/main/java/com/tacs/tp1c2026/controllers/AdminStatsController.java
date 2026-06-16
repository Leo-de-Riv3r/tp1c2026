package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresRole;
import com.tacs.tp1c2026.entities.dto.statistics.output.MostWantedCardEntry;
import com.tacs.tp1c2026.entities.dto.statistics.output.OverviewDto;
import com.tacs.tp1c2026.entities.dto.statistics.output.TimeseriesDto;
import com.tacs.tp1c2026.entities.dto.statistics.output.TopAuctionByOffersEntry;
import com.tacs.tp1c2026.entities.dto.statistics.output.TopExchangedCardEntry;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.services.AdminStatsService;
import com.tacs.tp1c2026.services.StatsSnapshotService;
import com.tacs.tp1c2026.services.StatsSnapshotService.Period;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dashboard del admin: stats del sistema. Tres capas por naturaleza del dato:
 * <ul>
 *   <li><b>Capa 1</b> — counts puntuales live: {@code /overview}.</li>
 *   <li><b>Capa 2</b> — counts por período (snapshot diario + delta):
 *       {@code /auctions}, {@code /proposals}, {@code /exchanges}.</li>
 *   <li><b>Capa 3</b> — top-N (live + cache TTL): {@code /most-wanted-cards}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;
    private final StatsSnapshotService statsSnapshotService;

    public AdminStatsController(AdminStatsService adminStatsService,
                                StatsSnapshotService statsSnapshotService) {
        this.adminStatsService = adminStatsService;
        this.statsSnapshotService = statsSnapshotService;
    }

    /** Counts puntuales (Capa 1). Cuatro {@code count()} con índice, live. */
    @RequiresRole("ADMIN")
    @GetMapping("/overview")
    public ResponseEntity<OverviewDto> getOverview() {
        return ResponseEntity.ok(adminStatsService.getOverview());
    }

    /** Subastas creadas en el período (Capa 2). */
    @RequiresRole("ADMIN")
    @GetMapping("/auctions")
    public ResponseEntity<TimeseriesDto> getAuctionsTimeseries(@RequestParam(defaultValue = "week") String period) {
        return ResponseEntity.ok(statsSnapshotService.getAuctionsTimeseries(parsePeriod(period)));
    }

    /** Propuestas creadas en el período (Capa 2). */
    @RequiresRole("ADMIN")
    @GetMapping("/proposals")
    public ResponseEntity<TimeseriesDto> getProposalsTimeseries(@RequestParam(defaultValue = "week") String period) {
        return ResponseEntity.ok(statsSnapshotService.getProposalsTimeseries(parsePeriod(period)));
    }

    /** Intercambios concretados en el período (Capa 2). */
    @RequiresRole("ADMIN")
    @GetMapping("/exchanges")
    public ResponseEntity<TimeseriesDto> getExchangesTimeseries(@RequestParam(defaultValue = "week") String period) {
        return ResponseEntity.ok(statsSnapshotService.getExchangesTimeseries(parsePeriod(period)));
    }

    /** Cartas más buscadas en los últimos {@code days} días (Capa 3, cacheado 5 min). */
    @RequiresRole("ADMIN")
    @GetMapping("/most-wanted-cards")
    public ResponseEntity<List<MostWantedCardEntry>> getMostWantedCards(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(adminStatsService.getMostWantedCardsInLastDays(days));
    }

    /** Cartas más intercambiadas en los últimos {@code days} días (Capa 3, cacheado 5 min). */
    @RequiresRole("ADMIN")
    @GetMapping("/top-exchanged-cards")
    public ResponseEntity<List<TopExchangedCardEntry>> getTopExchangedCards(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(adminStatsService.getTopExchangedCardsInLastDays(days));
    }

    /** Subasta activa con más ofertas PENDING (Capa 3, cacheado 5 min). 204 si no hay ninguna. */
    @RequiresRole("ADMIN")
    @GetMapping("/top-auction-by-offers")
    public ResponseEntity<TopAuctionByOffersEntry> getTopAuctionByOffers() {
        return adminStatsService.getTopAuctionByOffers()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private Period parsePeriod(String raw) {
        try {
            return Period.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadInputException("period debe ser uno de: day, week, month");
        }
    }
}
