package com.tacs.tp1c2026.entities.dto.settings;

import com.tacs.tp1c2026.entities.settings.AppSettings;

/**
 * DTO de configuración global. Se usa tanto en la respuesta del GET como en el body del PUT
 * (el admin envía el nuevo {@code maxPendingProposals}).
 */
public record SettingsDto(int maxPendingProposals) {

  public static SettingsDto from(AppSettings settings) {
    return new SettingsDto(settings.getMaxPendingProposals());
  }
}
