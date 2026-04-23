package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;

public class AlertaPorpuestaRecibidaDto extends AlertaDto {

  private final Integer propuestaId;
  private final Integer publicacionId;
  private final List<Integer> figuritaNumeros;

  public AlertaPorpuestaRecibidaDto(
      Integer id,
      Integer propuestaId,
      Integer publicacionId,
      List<Integer> figuritaNumeros) {
    super(id, "PROPUESTA_RECIBIDA");
    this.propuestaId = propuestaId;
    this.publicacionId = publicacionId;
    this.figuritaNumeros = figuritaNumeros;
  }


  public Integer getPropuestaId() {
    return propuestaId;
  }

  public Integer getPublicacionId() {
    return publicacionId;
  }

  public List<Integer> getFiguritaNumeros() {
    return figuritaNumeros;
  }
}
