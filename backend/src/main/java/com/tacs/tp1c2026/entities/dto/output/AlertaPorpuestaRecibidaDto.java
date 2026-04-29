package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;
import lombok.Getter;

@Getter
public class AlertaPorpuestaRecibidaDto extends AlertaDto {

  private final String propuestaId;
  private final String publicacionId;
  private final List<Integer> figuritaNumeros;

  public AlertaPorpuestaRecibidaDto(
      String id,
      String propuestaId,
      String publicacionId,
      List<Integer> figuritaNumeros) {
    super(id, "PROPUESTA_RECIBIDA");
    this.propuestaId = propuestaId;
    this.publicacionId = publicacionId;
    this.figuritaNumeros = figuritaNumeros;
  }

}
