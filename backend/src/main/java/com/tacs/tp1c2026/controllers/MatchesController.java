package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.match.UpcomingMatch;
import com.tacs.tp1c2026.services.UpcomingMatchesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bonus: próximos partidos del Mundial. Lee del cache (poblado por cron diario), sin pegarle a
 * API-Football en cada request. El widget del Home lo consume.
 */
@RestController
@RequestMapping("/matches")
public class MatchesController {

  private final UpcomingMatchesService service;

  public MatchesController(UpcomingMatchesService service) {
    this.service = service;
  }

  @GetMapping("/upcoming")
  public ResponseEntity<List<UpcomingMatch>> getUpcoming() {
    return ResponseEntity.ok(service.getCachedMatches());
  }
}
