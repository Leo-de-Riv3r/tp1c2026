package com.tacs.tp1c2026.entities.dto.output;

import java.time.LocalDateTime;

public class AlertaSubastaProximaDto extends AlertaDto {

  private final String subastaId;
  private final String figuritaId;
  private final Integer figuritaNumero;
  private final LocalDateTime fechaCierre;

  public AlertaSubastaProximaDto(
      String id,
      String subastaId,
      String figuritaId,
      Integer figuritaNumero,
      LocalDateTime fechaCierre) {
    super(id, "SUBASTA_PROXIMA");
    this.subastaId = subastaId;
    this.figuritaId = figuritaId;
    this.figuritaNumero = figuritaNumero;
    this.fechaCierre = fechaCierre;
  }

  public String getSubastaId() {
    return subastaId;
  }

  public String getFiguritaId() {
    return figuritaId;
  }

  public Integer getFiguritaNumero() {
    return figuritaNumero;
  }

  public LocalDateTime getFechaCierre() {
    return fechaCierre;
  }
}
