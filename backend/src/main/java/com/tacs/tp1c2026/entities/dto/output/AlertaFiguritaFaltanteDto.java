package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.CardCategory;

public class AlertaFiguritaFaltanteDto extends AlertaDto {

  private final String figuritaId;
  private final Integer figuritaNumero;
  private final String figuritaDescripcion;
  private final String figuritaPais;
  private final String figuritaEquipo;
  private final CardCategory figuritaCategoria;

  public AlertaFiguritaFaltanteDto(
      String id,
      String figuritaId,
      Integer figuritaNumero,
      String figuritaDescripcion,
      String figuritaPais,
      String figuritaEquipo,
      CardCategory figuritaCategoria) {
    super(id, "FIGURITA_FALTANTE");
    this.figuritaId = figuritaId;
    this.figuritaNumero = figuritaNumero;
    this.figuritaDescripcion = figuritaDescripcion;
    this.figuritaPais = figuritaPais;
    this.figuritaEquipo = figuritaEquipo;
    this.figuritaCategoria = figuritaCategoria;
  }


  public String getFiguritaId() {
    return figuritaId;
  }

  public Integer getFiguritaNumero() {
    return figuritaNumero;
  }

  public String getFiguritaDescripcion() {
    return figuritaDescripcion;
  }

  public String getFiguritaPais() {
    return figuritaPais;
  }

  public String getFiguritaEquipo() {
    return figuritaEquipo;
  }

  public CardCategory getFiguritaCategoria() {
    return figuritaCategoria;
  }
}
