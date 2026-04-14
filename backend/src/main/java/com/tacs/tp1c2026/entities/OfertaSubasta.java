package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoOfertaSubasta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class OfertaSubasta {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "subasta_id", referencedColumnName = "id")
  private Subasta subasta;

  @ManyToOne
  @JoinColumn(name = "postor_id", referencedColumnName = "id")
  private Usuario usuarioPostor;

  @ManyToMany
  @JoinTable(
      name = "oferta_subasta_figuritas",
      joinColumns = @JoinColumn(name = "oferta_subasta_id"),
      inverseJoinColumns = @JoinColumn(name = "figurita_id"))
  private List<Figurita> figuritasOfrecidas = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column
  private EstadoOfertaSubasta estado = EstadoOfertaSubasta.PENDIENTE;

  public OfertaSubasta(Usuario postor, List<Figurita> figuritasOfrecidas) {
    this.usuarioPostor = postor;
    this.figuritasOfrecidas = figuritasOfrecidas;
  }

  public void aceptar() {
    this.estado = EstadoOfertaSubasta.ACEPTADA;
  }

  public void rechazar() {
    this.estado = EstadoOfertaSubasta.RECHAZADA;
  }
}
