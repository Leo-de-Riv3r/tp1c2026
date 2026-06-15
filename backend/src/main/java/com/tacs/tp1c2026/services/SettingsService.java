package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.settings.AppSettings;
import com.tacs.tp1c2026.repositories.SettingsRepository;
import org.springframework.stereotype.Service;

/**
 * Acceso a la configuración global ({@link AppSettings}). Si el documento singleton todavía no
 * existe, devuelve uno con los defaults (sin persistirlo hasta que el admin lo modifique).
 */
@Service
public class SettingsService {

  private final SettingsRepository settingsRepository;

  public SettingsService(SettingsRepository settingsRepository) {
    this.settingsRepository = settingsRepository;
  }

  public AppSettings get() {
    return settingsRepository.findById(AppSettings.SINGLETON_ID).orElseGet(AppSettings::new);
  }

  public int getMaxPendingProposals() {
    return get().getMaxPendingProposals();
  }

  public int getMaxOffersPerAuction() {
    return get().getMaxOffersPerAuction();
  }

  public AppSettings setMaxPendingProposals(int value) {
    AppSettings settings = get();
    settings.setMaxPendingProposals(value); // valida >= 1
    return settingsRepository.save(settings);
  }

  public AppSettings setMaxOffersPerAuction(int value) {
    AppSettings settings = get();
    settings.setMaxOffersPerAuction(value); // valida 1 <= value <= MAX_OFFERS_HARD_CAP
    return settingsRepository.save(settings);
  }
}
