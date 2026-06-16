package com.tacs.tp1c2026.entities.dto.settings;

import com.tacs.tp1c2026.entities.settings.AppSettings;

/**
 * DTO de configuración global. Se usa tanto en la respuesta del GET como en el body del PUT
 * (el admin envía los nuevos caps de propuestas y ofertas).
 */
public record SettingsDto(int maxPendingProposals, int maxOffersPerAuction) {

  public static SettingsDto from(AppSettings settings) {
    return new SettingsDto(settings.getMaxPendingProposals(), settings.getMaxOffersPerAuction());
  }
}
