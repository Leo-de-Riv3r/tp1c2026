package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoOfertaSubasta;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "ofertas_subasta")
@TypeAlias("ofertaSubasta")
@Getter
@Setter
@NoArgsConstructor
public class OfertaSubasta {
  @Id
  private Integer id;

  @DocumentReference
  private Subasta subasta;

  @DocumentReference
  private Usuario usuarioPostor;

  private List<ItemOfertaSubasta> itemsOfrecidos = new ArrayList<>();

  private EstadoOfertaSubasta estado = EstadoOfertaSubasta.PENDIENTE;

  public OfertaSubasta(Usuario postor, Subasta subasta, List<ItemOfertaSubasta> ofertas) {
    this.setUsuarioPostor(postor);
    this.setSubasta(subasta);
    this.itemsOfrecidos = ofertas;
  }

  public void agregarItem(ItemOfertaSubasta item) {
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
