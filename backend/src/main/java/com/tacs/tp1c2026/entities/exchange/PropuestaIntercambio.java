/*package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoPropuesta;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Entity
@Table
@Getter
public class PropuestaIntercambio {
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  @ManyToOne
  @JoinColumn(name = "publicacion_id", referencedColumnName = "id")
  private PublicacionIntercambio publicacion;
  @OneToMany
  @JoinColumn(name = "figurita_id", referencedColumnName = "id")
  private List<Figurita> figuritas = new ArrayList<>();
  @ManyToOne
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private Usuario usuario;
  private EstadoPropuesta estado = EstadoPropuesta.PENDIENTE;

  public void rechazar() {
    this.estado = EstadoPropuesta.RECHAZADA;
  }

  public void aceptar() {
    this.estado = EstadoPropuesta.ACEPTADA;
  }
}
*/