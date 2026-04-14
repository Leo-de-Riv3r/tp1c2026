package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("SUBASTA_PROXIMA")
public class AlertaSubastaProxima extends Alerta {

  @ManyToOne
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private Usuario usuario;

  @ManyToOne
  @JoinColumn(name = "subasta_id", referencedColumnName = "id")
  private Subasta subasta;

  protected AlertaSubastaProxima() {}

  public AlertaSubastaProxima(Usuario usuario, Subasta subasta) {
    this.usuario = usuario;
    this.subasta = subasta;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public Subasta getSubasta() {
    return subasta;
  }

  @Override
  public AlertaDto visit(AlertaVisitor visitor) {
    return visitor.visit(this);
  }

}
