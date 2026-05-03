package com.tacs.tp1c2026.entities.dto.alert.output;

import java.util.List;

public class AlertaPorpuestaRecibidaDto extends AlertDto {

  private final String fromUserId;
  private final String fromUserName;
  private final Integer propuestaId;
  private final Integer publicacionId;
  private final List<Integer> figuritaNumeros;

  public AlertaPorpuestaRecibidaDto(
      Integer id,
      String fromUserId,
      String fromUserName,
      Integer propuestaId,
      Integer publicacionId,
      List<Integer> figuritaNumeros) {
    super(id, "PROPUESTA_RECIBIDA");
    this.fromUserId = fromUserId;
    this.fromUserName = fromUserName;
    this.propuestaId = propuestaId;
    this.publicacionId = publicacionId;
    this.figuritaNumeros = figuritaNumeros;
  }

  public String getFromUserId() {
    return fromUserId;
  }

  public String getFromUserName() {
    return fromUserName;
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
