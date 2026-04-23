package com.tacs.tp1c2026.entities.dto.output;

public class AlertaFiguritaFaltanteDto extends AlertaDto {

  private final Integer figuritaId;
  private final Integer figuritaNumero;
  private final String figuritaDescripcion;
  private final String figuritaPais;
  private final String figuritaEquipo;
  private final String figuritaCategoria;

  public AlertaFiguritaFaltanteDto(
      Integer id,
      Integer figuritaId,
      Integer figuritaNumero,
      String figuritaDescripcion,
      String figuritaPais,
      String figuritaEquipo,
      String figuritaCategoria) {
    super(id, "FIGURITA_FALTANTE");
    this.figuritaId = figuritaId;
    this.figuritaNumero = figuritaNumero;
    this.figuritaDescripcion = figuritaDescripcion;
    this.figuritaPais = figuritaPais;
    this.figuritaEquipo = figuritaEquipo;
    this.figuritaCategoria = figuritaCategoria;
  }


  public Integer getFiguritaId() {
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

  public String getFiguritaCategoria() {
    return figuritaCategoria;
  }
}
