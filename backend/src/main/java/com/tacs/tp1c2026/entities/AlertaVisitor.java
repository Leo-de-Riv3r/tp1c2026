package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaFiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaPorpuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaSubastaProximaDto;
import org.springframework.stereotype.Component;

@Component
public class AlertaVisitor {

  public AlertaDto visit(AlertaFiguritaFaltante alerta) {
    Figurita figurita = alerta.getFigurita();
    return new AlertaFiguritaFaltanteDto(
        alerta.getId(),
        figurita != null ? figurita.getId() : null,
        figurita != null ? figurita.getNumber() : null,
        figurita != null ? figurita.getDescription() : null,
        figurita != null ? figurita.getCountry() : null,
        figurita != null ? figurita.getTeam() : null,
        figurita != null ? figurita.getCategory() : null
    );
  }

  public AlertaDto visit(AlertaPropuestaRecibida alerta) {
    return new AlertaPorpuestaRecibidaDto(
        alerta.getId(),
        alerta.getPropuestaId(),
        alerta.getPublicacionId(),
        alerta.getFiguritaNumeros()
    );
  }

  public AlertaDto visit(AlertaSubastaProxima alerta) {
    Figurita figurita = alerta.getFigurita();
    return new AlertaSubastaProximaDto(
        alerta.getId(),
        alerta.getSubastaId(),
        figurita != null ? figurita.getId() : null,
        figurita != null ? figurita.getNumber() : null,
        alerta.getFechaCierre()
    );
  }
}
