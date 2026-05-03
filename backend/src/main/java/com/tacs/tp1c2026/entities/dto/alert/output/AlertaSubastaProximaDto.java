package com.tacs.tp1c2026.entities.dto.alert.output;

import java.time.LocalDateTime;

public class AlertaSubastaProximaDto extends AlertDto {

  private final Integer subastaId;
  private final String figuritaId;
  private final Integer figuritaNumero;
  private final LocalDateTime fechaCierre;

  public AlertaSubastaProximaDto(
      Integer id,
      Integer subastaId,
      String figuritaId,
      Integer figuritaNumero,
      LocalDateTime fechaCierre) {
    super(id, "SUBASTA_PROXIMA");
    this.subastaId = subastaId;
    this.figuritaId = figuritaId;
    this.figuritaNumero = figuritaNumero;
    this.fechaCierre = fechaCierre;
  }

  public Integer getSubastaId() {
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
