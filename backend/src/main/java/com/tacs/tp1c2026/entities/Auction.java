package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.AuctionState;
import com.tacs.tp1c2026.entities.enums.CardCategory;
import com.tacs.tp1c2026.exceptions.ConflictException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Document(collection = "subastas")
@TypeAlias("subasta")
@Getter
@Builder
public class Auction {
  @Id
  private String id;

  @DocumentReference
  private Card card;
  private Integer cardNumber;
  private String cardName;
  private String cardTeam;
  private String cardCountry;
  private CardCategory cardCategory;
  @DocumentReference
  private Usuario publisherUser;

  @Builder.Default
  private LocalDateTime createdDate = LocalDateTime.now();

  private LocalDateTime finishDate;

  private Integer minCardsRequired;
  @Builder.Default
  private AuctionState state = AuctionState.ACTIVA;

  private AuctionOffer mejorOferta;
  @Builder.Default
  private List<AuctionOffer> ofertas = new ArrayList<>();

  @DocumentReference
  @Builder.Default
  private List<Usuario> usuariosInteresados = new ArrayList<>();


  public void addOffer(AuctionOffer offer) {
    if (offer.getOfferedCards().size() < minCardsRequired) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Debes ofrecer al menos " + minCardsRequired + " figuritas."
      );
    }
    this.mejorOferta = offer;
  }

  public void addInterested(Usuario usuario) {
    this.usuariosInteresados.add(usuario);
  }

  public Boolean isExpired() {
    if (this.finishDate == null) return false;
    return this.finishDate.isBefore(LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")));
  }

  public Boolean isFinished() {
    return this.state == AuctionState.FINALIZADA;
  }

  public void finish() {
    if(this.isFinished()) {
      throw new ConflictException("La subasta ya finalizo");
    }
    this.state = AuctionState.FINALIZADA;
  }

  public void cancel() {
    if(this.isFinished()) {
      throw new ConflictException("La subasta ya finalizo");
    }
    this.state = AuctionState.CANCELADA;
  }
}