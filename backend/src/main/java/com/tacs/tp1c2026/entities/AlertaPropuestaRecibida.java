package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "alertas")
@TypeAlias("alertaPropuestaRecibida")
public class AlertaPropuestaRecibida extends Alerta {

  private Integer propuestaId;
  private Integer publicacionId;
  private List<Integer> figuritaNumeros = List.of();

  public AlertaPropuestaRecibida(PropuestaIntercambio propuesta) {
    this.propuestaId = propuesta != null ? propuesta.getId() : null;
    this.publicacionId = propuesta != null && propuesta.getPublicacion() != null ? propuesta.getPublicacion().getId() : null;
    this.figuritaNumeros = propuesta == null || propuesta.getFiguritas() == null
        ? List.of()
        : propuesta.getFiguritas().stream()
            .filter(Objects::nonNull)
            .map(Figurita::getNumber)
            .filter(Objects::nonNull)
            .toList();
  }

  @Override
  public AlertaDto visit(AlertaVisitor visitor) {
    return visitor.visit(this);
  }
}
