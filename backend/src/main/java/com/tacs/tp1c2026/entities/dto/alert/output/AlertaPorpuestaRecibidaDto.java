package com.tacs.tp1c2026.entities.dto.alert.output;

import java.util.List;

public class AlertaPorpuestaRecibidaDto extends AlertDto {

  private final String fromUserId;
  private final String fromUserName;
  private final String propuestaId;
  private final String publicacionId;
  private final List<Integer> figuritaNumeros;

  public AlertaPorpuestaRecibidaDto(
      Integer id,
      String fromUserId,
      String fromUserName,
      String propuestaId,
      String publicacionId,
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

  public String getPropuestaId() {
    return propuestaId;
  }

  public String getPublicacionId() {
    return publicacionId;
  }

  public List<Integer> getFiguritaNumeros() {
    return figuritaNumeros;
  }
}
