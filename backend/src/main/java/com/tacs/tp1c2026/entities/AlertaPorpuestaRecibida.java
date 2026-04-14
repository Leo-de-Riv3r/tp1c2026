package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("PROPUESTA_RECIBIDA")
public class AlertaPorpuestaRecibida extends Alerta {

  @ManyToOne
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private Usuario usuario;

  @ManyToOne
  @JoinColumn(name = "propuesta_id", referencedColumnName = "id")
  private PropuestaIntercambio propuesta;

  protected AlertaPorpuestaRecibida() {}

  public AlertaPorpuestaRecibida(Usuario usuario, PropuestaIntercambio propuesta) {
    this.usuario = usuario;
    this.propuesta = propuesta;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public PropuestaIntercambio getPropuesta() {
    return propuesta;
  }

  @Override
  public AlertaDto visit(AlertaVisitor visitor) {
    return visitor.visit(this);
  }

}
