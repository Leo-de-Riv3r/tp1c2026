package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresRole;
import com.tacs.tp1c2026.entities.dto.statistics.output.MostWantedCardEntry;
import com.tacs.tp1c2026.entities.dto.statistics.output.OverviewDto;
import com.tacs.tp1c2026.services.AdminStatsService;
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
 *   <li><b>Capa 2</b> — counts por período (snapshot diario + delta): por implementar.</li>
 *   <li><b>Capa 3</b> — top-N (live + cache TTL): {@code /most-wanted-cards}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    /** Counts puntuales (Capa 1). Cuatro {@code count()} con índice, live. */
    @RequiresRole("ADMIN")
    @GetMapping("/overview")
    public ResponseEntity<OverviewDto> getOverview() {
        return ResponseEntity.ok(adminStatsService.getOverview());
    }

    /** Cartas más buscadas en los últimos {@code days} días (Capa 3). */
    @RequiresRole("ADMIN")
    @GetMapping("/most-wanted-cards")
    public ResponseEntity<List<MostWantedCardEntry>> getMostWantedCards(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(adminStatsService.getMostWantedCardsInLastDays(days));
    }
}
