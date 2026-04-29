package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;

@Getter
@Setter
@NoArgsConstructor
@TypeAlias("alertaPropuestaRecibida")
public class AlertaPropuestaRecibida extends Alerta {
  private String propuestaId;
  private String publicacionId;
  private List<Integer> figuritaNumeros = new ArrayList<>();

  public AlertaPropuestaRecibida(ExchangeProposal propuesta) {
    this.propuestaId = propuesta != null ? propuesta.getId() : null;
    this.publicacionId = propuesta != null && propuesta.getPublication() != null ? propuesta.getPublication().getId() : null;
    this.figuritaNumeros = propuesta == null || propuesta.getCards() == null
        ? List.of()
        : propuesta.getCards().stream()
            .filter(Objects::nonNull)
            .map(Card::getNumber)
            .filter(Objects::nonNull)
            .toList();
  }

  @Override
  public AlertaDto visit(AlertaVisitor visitor) {
    return visitor.visit(this);
  }
}
