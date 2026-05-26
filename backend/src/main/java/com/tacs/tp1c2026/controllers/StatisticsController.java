package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresRole;
import com.tacs.tp1c2026.entities.dto.statistics.output.MostWantedCardEntry;
import com.tacs.tp1c2026.services.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * Cartas más buscadas (marcadas como missing) en los últimos {@code days} días.
     * Endpoint pensado para US12 (estadísticas admin).
     *
     * @param days ventana en días (default 7)
     */
    @RequiresRole("ADMIN")
    @GetMapping("/most-wanted-cards")
    public ResponseEntity<List<MostWantedCardEntry>> getMostWantedCards(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(statisticsService.getMostWantedCardsInLastDays(days));
    }
}
