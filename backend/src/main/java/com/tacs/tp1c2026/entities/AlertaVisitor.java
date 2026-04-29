package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaFiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaPorpuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaSubastaProximaDto;
import org.springframework.stereotype.Component;

@Component
public class AlertaVisitor {

  public AlertaDto visit(AlertaFiguritaFaltante alerta) {
    Card card = alerta.getCard();
    return new AlertaFiguritaFaltanteDto(
        alerta.getId(),
        card != null ? card.getId() : null,
        card != null ? card.getNumber() : null,
        card != null ? card.getDescription() : null,
        card != null ? card.getCountry() : null,
        card != null ? card.getTeam() : null,
        card != null ? card.getCategory() : null
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
    Card card = alerta.getCard();
    return new AlertaSubastaProximaDto(
        alerta.getId(),
        alerta.getSubastaId(),
        card != null ? card.getId() : null,
        card != null ? card.getNumber() : null,
        alerta.getFechaCierre()
    );
  }
}
