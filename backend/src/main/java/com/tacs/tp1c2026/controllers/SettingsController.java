package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresRole;
import com.tacs.tp1c2026.entities.dto.settings.SettingsDto;
import com.tacs.tp1c2026.services.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuración global, solo para el admin. Hoy expone el tope de propuestas pendientes por
 * publicación: GET para visualizarlo, PUT para modificarlo.
 */
@RestController
@RequestMapping("/settings")
public class SettingsController {

  private final SettingsService settingsService;

  public SettingsController(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  /** Devuelve la configuración global actual (hoy, el tope de propuestas pendientes). Solo admin. */
  @RequiresRole("ADMIN")
  @GetMapping
  public ResponseEntity<SettingsDto> getSettings() {
    return ResponseEntity.ok(SettingsDto.from(settingsService.get()));
  }

  /** Actualiza la configuración global (valida {@code maxPendingProposals >= 1}). Solo admin. */
  @RequiresRole("ADMIN")
  @PutMapping
  public ResponseEntity<SettingsDto> updateSettings(@RequestBody SettingsDto body) {
    return ResponseEntity.ok(
        SettingsDto.from(settingsService.setMaxPendingProposals(body.maxPendingProposals())));
  }
}
