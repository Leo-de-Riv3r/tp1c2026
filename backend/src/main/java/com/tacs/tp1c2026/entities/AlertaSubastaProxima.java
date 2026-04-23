package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "alertas")
@TypeAlias("alertaSubastaProxima")
public class AlertaSubastaProxima extends Alerta {

  private Integer subastaId;
  private Figurita figurita;
  private LocalDateTime fechaCierre;

  public AlertaSubastaProxima(
      Integer subastaId,
      Figurita figurita,
      LocalDateTime fechaCierre) {
    this.subastaId = subastaId;
    this.figurita = figurita;
    this.fechaCierre = fechaCierre;
  }

  @Override
  public AlertaDto visit(AlertaVisitor visitor) {
    return visitor.visit(this);
  }
}
