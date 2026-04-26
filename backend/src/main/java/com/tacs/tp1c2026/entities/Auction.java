package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.exceptions.OfertaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.SubastaCerradaException;
import com.tacs.tp1c2026.entities.conditions.AuctionCondition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "auctions")
public class Auction {

  @Id
  @Getter
  private Integer id;

  @Setter
  @DocumentReference
  private User publisherUser;

  private final LocalDateTime creationDate;

  private LocalDateTime closeDate;

  private List<AuctionCondition> conditions = new ArrayList<>();

  private AuctionStatus status = AuctionStatus.ACTIVE;

  @Setter
  private AuctionOffer bestOffer;

  private final List<AuctionOffer> offers = new ArrayList<>();

  @DocumentReference
  private final List<User> interestedUsers = new ArrayList<>();

  public Auction(Integer auctionDurationHours, List<AuctionCondition> conditions) {
    this.setPublisherUser(publisherUser);
    this.creationDate = LocalDateTime.now();
    this.closeDate = this.creationDate.plusHours(auctionDurationHours);
    this.conditions = new ArrayList<>(conditions == null ? List.of() : conditions);
  }

  public void addOffer(AuctionOffer auctionOffer) {
    // Reserve the offered stickers from the bidder before accepting the offer
    auctionOffer.reserveItems();
    this.offers.add(auctionOffer);
  }

  /**
   * Convenience overload: create and add an offer from a bidder and a list of items.
   * Validation and reservation of items happens in the AuctionOffer / User domain logic.
   */
  public void addOffer(User bidder, List<AuctionItem> items) {
    items.forEach(i -> bidder.removeAuctionStickers(i.getSticker(), i.getAmount()));
    AuctionOffer offer = new AuctionOffer( bidder, items);
    this.addOffer(offer);
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

