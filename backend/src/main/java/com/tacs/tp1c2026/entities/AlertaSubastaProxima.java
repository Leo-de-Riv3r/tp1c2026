package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
@Setter
@NoArgsConstructor
@TypeAlias("alertaSubastaProxima")
public class AlertaSubastaProxima extends Alerta {

  private String subastaId;
  @DocumentReference
  private Card card;
  private LocalDateTime fechaCierre;

  public AlertaSubastaProxima(
      String subastaId,
      Card card,
      LocalDateTime fechaCierre) {
    this.subastaId = subastaId;
    this.card = card;
    this.fechaCierre = fechaCierre;
  }

  @Override
  public AlertaDto visit(AlertaVisitor visitor) {
    return visitor.visit(this);
  }
}
