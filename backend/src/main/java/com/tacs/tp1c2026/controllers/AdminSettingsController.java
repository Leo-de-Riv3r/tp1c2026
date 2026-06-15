package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresRole;
import com.tacs.tp1c2026.entities.dto.settings.SettingsDto;
import com.tacs.tp1c2026.entities.settings.AppSettings;
import com.tacs.tp1c2026.services.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parametría global del sistema, sólo para el admin. Bajo el namespace {@code /admin/*} junto
 * con el resto de los endpoints administrativos (stats, broadcast, etc.). Hoy maneja:
 * - {@code maxPendingProposals}: cap de propuestas PENDIENTES por publicación.
 * - {@code maxOffersPerAuction}: cap de ofertas PENDIENTES por subasta.
 *
 * <p>El PUT es full-replace: el body debe traer ambos campos.
 */
@RestController
@RequestMapping("/admin/settings")
public class AdminSettingsController {

  private final SettingsService settingsService;

  public AdminSettingsController(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @RequiresRole("ADMIN")
  @GetMapping
  public ResponseEntity<SettingsDto> getSettings() {
    return ResponseEntity.ok(SettingsDto.from(settingsService.get()));
  }

  @RequiresRole("ADMIN")
  @PutMapping
  public ResponseEntity<SettingsDto> updateSettings(@RequestBody SettingsDto body) {
    settingsService.setMaxPendingProposals(body.maxPendingProposals());
    AppSettings updated = settingsService.setMaxOffersPerAuction(body.maxOffersPerAuction());
    return ResponseEntity.ok(SettingsDto.from(updated));
  }
}
