package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import com.tacs.tp1c2026.exceptions.OfertaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.SubastaCerradaException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "subastas")
@TypeAlias("subasta")
@Getter
@Setter
@NoArgsConstructor
public class Auction {
  @Id
  private Integer id;

  private Figurita figurita;

  private Integer usuarioPublicanteId;

  @Transient
  private Usuario usuarioPublicante;

  private LocalDateTime fechaCreacion;

  private LocalDateTime fechaCierre;

  private Integer cantidadMinFiguritas;

  private EstadoSubasta estado = EstadoSubasta.ACTIVA;

  private Integer mejorOfertaId;

  @Transient
  private AuctionOffer mejorOferta;

  private List<AuctionOffer> ofertas = new ArrayList<>();

  private List<Integer> usuariosInteresadosIds = new ArrayList<>();

  public Auction(Usuario usuario, Figurita coleccion, Integer duracionSubasta, Integer cantidadMinFiguritas){
    this.setUsuarioPublicante(usuario);
    this.figurita = coleccion;
    this.fechaCreacion = LocalDateTime.now();
    this.fechaCierre = this.fechaCreacion.plusHours(duracionSubasta);
    this.cantidadMinFiguritas = cantidadMinFiguritas;
    this.estado = EstadoSubasta.ACTIVA;
  }

  public void setUsuarioPublicante(Usuario usuarioPublicante) {
    this.usuarioPublicante = usuarioPublicante;
    this.usuarioPublicanteId = usuarioPublicante != null ? usuarioPublicante.getId() : null;
  }

  public void setBestOffer(AuctionOffer mejorOferta) {
    this.mejorOferta = mejorOferta;
    this.mejorOfertaId = mejorOferta != null ? mejorOferta.getId() : null;
  }

  public void addOffer(AuctionOffer auctionOffer){
    if (this.ofertas == null) {
      this.ofertas = new ArrayList<>();
    }
    this.ofertas.add(auctionOffer);
  }

  public void rejectOffer(AuctionOffer oferta) throws OfertaYaProcesadaException, IllegalArgumentException {
    if (oferta.getAuction() == null || !Objects.equals(oferta.getAuction().getId(), this.id)) {
      throw new IllegalArgumentException("La oferta no corresponde a la subasta");
    }
    if (!oferta.isPending()) {
      throw new OfertaYaProcesadaException("La oferta ya fue aceptada o rechazada");
    }
    oferta.reject();
  }

  public boolean canAcceptOffers() {
    return !EstadoSubasta.CERRADA.equals(this.estado) && !EstadoSubasta.ADJUDICADA.equals(this.estado);
  }

  public void acceptOffer(AuctionOffer oferta) throws SubastaCerradaException, OfertaYaProcesadaException, IllegalArgumentException {
    if (!canAcceptOffers()) {
      throw new SubastaCerradaException("La subasta no permite aceptar ofertas");
    }
    if (!oferta.isPending()) {
      throw new OfertaYaProcesadaException("La oferta ya fue aceptada o rechazada");
    }
    if (oferta.getAuction() == null || !Objects.equals(oferta.getAuction().getId(), this.id)) {
      throw new IllegalArgumentException("La oferta no corresponde a la subasta");
    }

    oferta.accept();
    this.estado = EstadoSubasta.ADJUDICADA;
    this.setBestOffer(oferta);

    if (this.ofertas == null) {
      return;
    }

    this.ofertas.stream()
        .filter(o -> !Objects.equals(o.getId(), oferta.getId()))
        .filter(AuctionOffer::isPending)
        .forEach(AuctionOffer::reject);
  }

  public void addInterestedUser(Usuario usuario) {
    if (usuario == null || usuario.getId() == null) {
      return;
    }
    if (!this.usuariosInteresadosIds.contains(usuario.getId())) {
      this.usuariosInteresadosIds.add(usuario.getId());
    }
  }
}
