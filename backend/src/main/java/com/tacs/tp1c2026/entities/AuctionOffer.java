package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoOfertaSubasta;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "ofertas_subasta")
@TypeAlias("ofertaSubasta")
@Getter
@Setter
@NoArgsConstructor
public class AuctionOffer {
  @Id
  private Integer id;

  @DocumentReference
  private Auction auction;

  @DocumentReference
  private Usuario usuarioPostor;

  private List<AuctionOfferItems> itemsOfrecidos = new ArrayList<>();

  private EstadoOfertaSubasta estado = EstadoOfertaSubasta.PENDIENTE;

  public AuctionOffer(Usuario postor, Auction auction, List<AuctionOfferItems> ofertas) {
    this.setUsuarioPostor(postor);
    this.setAuction(auction);
    this.itemsOfrecidos = ofertas;
  }


  public void agregarItem(AuctionOfferItems item) {
    this.itemsOfrecidos.add(item);
  }

  public Integer getTotalFiguritas() {
    return this.itemsOfrecidos.stream()
        .map(AuctionOfferItems::getCantidad)
        .reduce(0, Integer::sum);
  }

  public boolean isPending() {
    return EstadoOfertaSubasta.PENDIENTE.equals(this.estado);
  }

  public void accept() {
    this.estado = EstadoOfertaSubasta.ACEPTADA;
  }

  public void reject() {
    this.estado = EstadoOfertaSubasta.RECHAZADA;
  }
}
