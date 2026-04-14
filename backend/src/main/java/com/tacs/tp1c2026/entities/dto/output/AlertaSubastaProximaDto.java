package com.tacs.tp1c2026.entities.dto.output;

import java.time.LocalDateTime;

public class AlertaSubastaProximaDto extends AlertaDto {

  private final Integer subastaId;
  private final Integer figuritaId;
  private final Integer figuritaNumero;
  private final LocalDateTime fechaCierre;

  public AlertaSubastaProximaDto(
      Integer id,
      Integer subastaId,
      Integer figuritaId,
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

  public Integer getFiguritaId() {
    return figuritaId;
  }

  public Integer getFiguritaNumero() {
    return figuritaNumero;
  }

  public LocalDateTime getFechaCierre() {
    return fechaCierre;
  }
}
