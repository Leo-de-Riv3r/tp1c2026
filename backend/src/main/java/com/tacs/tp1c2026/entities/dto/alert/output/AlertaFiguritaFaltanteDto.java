package com.tacs.tp1c2026.entities.dto.alert.output;

import com.tacs.tp1c2026.entities.enums.Category;

public class AlertaFiguritaFaltanteDto extends AlertDto {

  private final String fromUserId;
  private final String fromUserName;
  private final String figuritaId;
  private final Integer figuritaNumero;
  private final String figuritaJugador;
  private final String figuritaSeleccion;
  private final String figuritaEquipo;
  private final Category figuritaCategoria;

  public AlertaFiguritaFaltanteDto(
      Integer id,
      String fromUserId,
      String fromUserName,
      String figuritaId,
      Integer figuritaNumero,
      String figuritaJugador,
      String figuritaSeleccion,
      String figuritaEquipo,
      Category figuritaCategoria) {
    super(id, "FIGURITA_FALTANTE");
    this.fromUserId = fromUserId;
    this.fromUserName = fromUserName;
    this.figuritaId = figuritaId;
    this.figuritaNumero = figuritaNumero;
    this.figuritaJugador = figuritaJugador;
    this.figuritaSeleccion = figuritaSeleccion;
    this.figuritaEquipo = figuritaEquipo;
    this.figuritaCategoria = figuritaCategoria;
  }

  public String getFromUserId() {
    return fromUserId;
  }

  public String getFromUserName() {
    return fromUserName;
  }

  public String getFiguritaId() {
    return figuritaId;
  }

  public Integer getFiguritaNumero() {
    return figuritaNumero;
  }

  public String getFiguritaJugador() {
    return figuritaJugador;
  }

  public String getFiguritaSeleccion() {
    return figuritaSeleccion;
  }

  public String getFiguritaEquipo() {
    return figuritaEquipo;
  }

  public Category getFiguritaCategoria() {
    return figuritaCategoria;
  }
}
