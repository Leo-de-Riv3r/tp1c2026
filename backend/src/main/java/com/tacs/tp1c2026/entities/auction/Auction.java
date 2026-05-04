package com.tacs.tp1c2026.entities.auction;

import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.AuctionClosedException;
import com.tacs.tp1c2026.exceptions.OfferAlreadyProcessedException;
import com.tacs.tp1c2026.exceptions.OfferAlreadyRejectedException;
import com.tacs.tp1c2026.exceptions.OfferNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "auctions")
@Getter
public class Auction {

  @Id
  private String id;

  @DocumentReference
  private Card card;
  private Integer cardNumber;
  private String cardDescription;
  private String cardCountry;
  private String cardTeam;
  private Category cardCategory;

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

  public Auction(User user, Card card, Integer auctionDurationHours, List<AuctionCondition> conditions) {
    this.publisherUser = user;
    this.card = card;
    this.cardNumber = card.getNumber();
    this.cardDescription = card.getDescription();
    this.cardCountry = card.getCountry();
    this.cardTeam = card.getTeam();
    this.cardCategory = card.getCategory();
    this.creationDate = LocalDateTime.now();
    this.closeDate = this.creationDate.plusHours(auctionDurationHours);
    this.conditions = new ArrayList<>(conditions == null ? List.of() : conditions);
  }

  public void addOffer(AuctionOffer auctionOffer) {
    this.offers.add(auctionOffer);
  }

  public void rejectOffer(AuctionOffer offer) throws OfferNotFoundException, OfferAlreadyRejectedException {
    if (!offers.contains(offer)) {
      throw new OfferNotFoundException("Offer does not correspond to auction");
    }
    if (!offer.isPending()) {
      throw new OfferAlreadyRejectedException("Offer was already rejected");
    }
    offer.reject();
  }

  public boolean allowsOfferAcceptance() {
    return this.status == AuctionStatus.ACTIVE;
  }

  public void acceptOffer(AuctionOffer offer) throws AuctionClosedException, OfferAlreadyProcessedException, OfferNotFoundException {
    if (!allowsOfferAcceptance()) {
      throw new AuctionClosedException("The auction does not allow accepting offers");
    }
    if (!offer.isPending()) {
      throw new OfferAlreadyProcessedException("The offer has already been accepted or rejected");
    }
    if (!offers.contains(offer)) {
      throw new OfferNotFoundException("The offer does not correspond to this auction");
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

  public void cancel() throws AuctionClosedException {
    if (this.status != AuctionStatus.ACTIVE) {
      throw new AuctionClosedException("Solo se puede cancelar una subasta activa");
    }
    this.status = AuctionStatus.CANCELLED;
    this.offers.stream()
        .filter(AuctionOffer::isPending)
        .forEach(AuctionOffer::reject);
  }

  public boolean isExpired() {
    return this.closeDate != null && this.closeDate.isBefore(LocalDateTime.now());
  }
}
