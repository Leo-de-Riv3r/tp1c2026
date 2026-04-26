/*
package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaFiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaPorpuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaSubastaProximaDto;
import org.springframework.stereotype.Component;

@Component
public class AlertaVisitor {

  public AlertaDto visit(AlertaFiguritaFaltante alerta) {
    return new AlertaFiguritaFaltanteDto(
        alerta.getId(),
        alerta.getUsuario().getId(),
        alerta.getUsuario().getName(),
        alerta.getFigurita().getId(),
        alerta.getFigurita().getNumero(),
        alerta.getFigurita().getJugador(),
        alerta.getFigurita().getSeleccion(),
        alerta.getFigurita().getEquipo(),
        alerta.getFigurita().getCategoria()
    );
  }

  public AlertaDto visit(AlertaPorpuestaRecibida alerta) {
    return new AlertaPorpuestaRecibidaDto(
        alerta.getId(),
        alerta.getUsuario().getId(),
        alerta.getUsuario().getName(),
        alerta.getPropuesta().getId(),
        alerta.getPropuesta().getPublicacion().getId(),
        alerta.getPropuesta().getFiguritas().stream().map(Figurita::getNumero).toList()
    );
  }

  public AlertaDto visit(AlertaSubastaProxima alerta) {
    return new AlertaSubastaProximaDto(
        alerta.getId(),
        alerta.getSubasta().getId(),
        alerta.getSubasta().getFiguritaPublicada().getFigurita().getId(),
        alerta.getSubasta().getFiguritaPublicada().getFigurita().getNumero(),
        alerta.getSubasta().getFechaCierre()
    );
  }
}
*/
