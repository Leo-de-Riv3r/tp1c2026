package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.exceptions.OfertaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.SubastaCerradaException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "auctions")
public class Auction {
  @Id
  private Integer id;


  @Setter
  @DocumentReference
  private User publisherUser;

  private LocalDateTime creationDate;

  private LocalDateTime closeDate;

  private Integer minimumStickerCount;

  private AuctionStatus status = AuctionStatus.ACTIVE;

  @Setter
  private AuctionOffer bestOffer;

  private List<AuctionOffer> offers;

  @DocumentReference
  private final List<User> interestedUsers = new ArrayList<>();

  public Auction(Integer auctionDurationHours, Integer minimumStickerCount) {
    this.setPublisherUser(publisherUser);
    this.closeDate = this.creationDate.plusHours(auctionDurationHours);
    this.minimumStickerCount = minimumStickerCount;
  }

  public void addOffer(AuctionOffer auctionOffer) {
    this.offers.add(auctionOffer);
  }

  public void rejectOffer(AuctionOffer offer) throws OfertaYaProcesadaException, IllegalArgumentException {
    if (!offers.contains(offer)) {
      throw new IllegalArgumentException("La oferta no corresponde a la subasta");
    }
    if (!offer.isPending()) {
      throw new OfertaYaProcesadaException("La oferta ya fue aceptada o rechazada");
    }
    offer.reject();
  }

  public boolean allowsOfferAcceptance() {
    return this.status == AuctionStatus.ACTIVE;
  }

  public void acceptOffer(AuctionOffer offer) throws SubastaCerradaException, OfertaYaProcesadaException, IllegalArgumentException {
    if (!allowsOfferAcceptance()) {
      throw new SubastaCerradaException("La subasta no permite aceptar ofertas");
    }
    if (!offer.isPending()) {
      throw new OfertaYaProcesadaException("La oferta ya fue aceptada o rechazada");
    }
    if (!offers.contains(offer)) {
      throw new IllegalArgumentException("La oferta no corresponde a la subasta");
    }

    offer.accept();
    this.status = AuctionStatus.AWARDED;
    this.setBestOffer(offer);

    if (this.offers == null) {
      return;
    }

    this.offers.stream()
        .filter(AuctionOffer::isPending)
        .forEach(AuctionOffer::reject);
  }

  public void addInterestedUser(User user) {
    this.interestedUsers.add(user);
  }

  public boolean notCancelled() {
    return this.status != AuctionStatus.CANCELLED;
  }

}

