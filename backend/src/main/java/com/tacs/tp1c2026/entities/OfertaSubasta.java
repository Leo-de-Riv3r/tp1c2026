/*package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoOfertaSubasta;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @OneToMany(mappedBy = "ofertaSubasta", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ItemOfertaSubasta> itemsOfrecidos = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column
  private EstadoOfertaSubasta estado = EstadoOfertaSubasta.PENDIENTE;

  public OfertaSubasta(Usuario postor, Subasta subasta, List<ItemOfertaSubasta> ofertas) {
    this.usuarioPostor = postor;
    this.subasta = subasta;
    this.itemsOfrecidos = ofertas;
  }

  public void agregarItem(ItemOfertaSubasta item) {
    item.setOfertaSubasta(this);
    this.itemsOfrecidos.add(item);
  }

  public Integer getTotalFiguritas() {
    return this.itemsOfrecidos.stream()
        .map(ItemOfertaSubasta::getCantidad)
        .reduce(0, Integer::sum);
  }

  public boolean estaPendiente() {
    return EstadoOfertaSubasta.PENDIENTE.equals(this.estado);
  }

  public void aceptar() {
    this.estado = EstadoOfertaSubasta.ACEPTADA;
  }

  public void rechazar() {
    this.estado = EstadoOfertaSubasta.RECHAZADA;
  }
}
*/