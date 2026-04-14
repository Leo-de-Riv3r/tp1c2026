package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaFiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaPropuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaSubastaProximaDto;

public class AlertaVisitor {

  public AlertaDto visit(AlertaFiguritaFaltante alerta) {
    return new AlertaFiguritaFaltanteDto(
        alerta.getId(),
        alerta.getUsuario().getId(),
        alerta.getUsuario().getNombre(),
        alerta.getFigurita().getId(),
        alerta.getFigurita().getNumero(),
        alerta.getFigurita().getJugador(),
        alerta.getFigurita().getSeleccion(),
        alerta.getFigurita().getEquipo(),
        alerta.getFigurita().getCategoria()
    );
  }

  public AlertaDto visit(AlertaPorpuestaRecibida alerta) {
    return new AlertaPropuestaRecibidaDto(
        alerta.getId(),
        alerta.getUsuario().getId(),
        alerta.getUsuario().getNombre(),
        alerta.getPropuesta().getId(),
        alerta.getPropuesta().getPublicacion().getId(),
        alerta.getPropuesta().getFiguritas().stream().map(Figurita::getNumero).toList()
    );
  }

  public AlertaDto visit(AlertaSubastaProxima alerta) {
    return new AlertaSubastaProximaDto(
        alerta.getId(),
        alerta.getSubasta().getId(),
        alerta.getSubasta().getFigurita().getId(),
        alerta.getSubasta().getFigurita().getNumero(),
        alerta.getSubasta().getFechaCierre()
    );
  }
}
