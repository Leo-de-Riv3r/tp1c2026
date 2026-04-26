package com.tacs.tp1c2026.entities.dto.alert.output;

import com.tacs.tp1c2026.entities.enums.Categoria;

public class AlertaFiguritaFaltanteDto extends AlertaDto {

  private final Integer fromUserId;
  private final String fromUserName;
  private final Integer figuritaId;
  private final Integer figuritaNumero;
  private final String figuritaJugador;
  private final String figuritaSeleccion;
  private final String figuritaEquipo;
  private final Categoria figuritaCategoria;

  public AlertaFiguritaFaltanteDto(
      Integer id,
      Integer fromUserId,
      String fromUserName,
      Integer figuritaId,
      Integer figuritaNumero,
      String figuritaJugador,
      String figuritaSeleccion,
      String figuritaEquipo,
      Categoria figuritaCategoria) {
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

  public Integer getFromUserId() {
    return fromUserId;
  }

  public String getFromUserName() {
    return fromUserName;
  }

  public Integer getFiguritaId() {
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

  public Categoria getFiguritaCategoria() {
    return figuritaCategoria;
  }
}
