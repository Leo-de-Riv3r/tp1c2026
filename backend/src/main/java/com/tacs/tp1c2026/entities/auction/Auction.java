package com.tacs.tp1c2026.entities.auction;

import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.CardType;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.AuctionClosedException;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import com.tacs.tp1c2026.exceptions.OfferAlreadyProcessedException;
import com.tacs.tp1c2026.exceptions.OfferAlreadyRejectedException;
import com.tacs.tp1c2026.exceptions.OfferNotFoundException;
import com.tacs.tp1c2026.exceptions.UnprocessableException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "auctions")
@Getter
@TypeAlias("auction")
public class Auction {

  @Id
  private String id;

  @Version
  private Long version;

  @DocumentReference
  private Card card;
  private String cardId;
  private Integer cardNumber;
  private String cardDescription;
  private String cardCountry;
  private String cardTeam;
  private Category cardCategory;
  private CardType cardType;

  @Setter
  @DocumentReference
  private User publisherUser;

  // Snapshot of publisher (denormalized) — avoids join on read
  private String publisherName;
  private String publisherAvatarId;

  private LocalDateTime creationDate;

  private LocalDateTime closeDate;

  private List<AuctionCondition> conditions = new ArrayList<>();

  private AuctionStatus status = AuctionStatus.ACTIVE;

  @Setter
  private AuctionOffer bestOffer;

  private List<AuctionOffer> offers = new ArrayList<>();

  @DocumentReference
  private List<User> interestedUsers = new ArrayList<>();

  protected Auction() {
    // No-arg constructor para que Spring Data Mongo pueda hidratar via field reflection.
  }

  public Auction(User user, Card card, Integer auctionDurationHours, List<AuctionCondition> conditions) {
    this.publisherUser = user;
    this.publisherName = user.getName();
    this.publisherAvatarId = user.getAvatarId();
    this.card = card;
    this.cardId = card.getId();
    this.cardNumber = card.getNumber();
    this.cardDescription = card.getDescription();
    this.cardCountry = card.getCountry();
    this.cardTeam = card.getTeam();
    this.cardCategory = card.getCategory();
    this.cardType = card.getType();
    this.creationDate = LocalDateTime.now();
    this.closeDate = this.creationDate.plusHours(auctionDurationHours);
    this.conditions = new ArrayList<>(conditions == null ? List.of() : conditions);
  }

  public void addOffer(AuctionOffer auctionOffer) {
    checkConditions(auctionOffer);
    this.offers.add(auctionOffer);
  }

  public boolean checkConditions(AuctionOffer auctionOffer) {
    for (AuctionCondition condition : conditions) {
      if (!condition.canOffer(auctionOffer.getBidder(), auctionOffer)) {
        throw new UnprocessableException("You do not meet the minimum conditions to bid");
      }
    }
    return true;
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
    findOfferById(offer.getId());
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

  /**
   * Cancela la subasta como invariante atómico:
   *   - Cambia status a CANCELLED.
   *   - Rechaza todas las offers que estaban PENDING (las que ya estaban REJECTED no se tocan).
   *
   * <p>Devuelve un {@link CancelResult} con (a) los commits a liberar — publisher + bidders cuyas
   * offers se rechazan en este paso — y (b) los bidder IDs a los que hay que notificar la
   * cancelación. El service aplica esos cambios contra el {@code UserRepository} y dispara las
   * notificaciones.
   */
  public CancelResult cancel() {
    if (this.status != AuctionStatus.ACTIVE) {
      throw new AuctionClosedException("Sólo se puede cancelar una subasta activa");
    }
    if (isExpired()) {
      throw new AuctionClosedException("La subasta ya expiró");
    }
    this.status = AuctionStatus.CANCELLED;

    List<CommitRelease> releases = new ArrayList<>();
    List<String> notifyBidderIds = new ArrayList<>();

    // Publisher recupera 1 unidad de la card subastada (la que comprometió al crear la subasta).
    releases.add(new CommitRelease(this.publisherUser.getId(), this.card.getId(), 1));

    for (AuctionOffer offer : this.offers) {
      if (!offer.isPending()) continue;
      offer.reject();
      notifyBidderIds.add(offer.getBidderId());
      for (AuctionItem oi : offer.getOfferedItems()) {
        releases.add(new CommitRelease(offer.getBidderId(), oi.getCard().getId(), oi.getAmount()));
      }
    }

    return new CancelResult(releases, notifyBidderIds);
  }

  public boolean isExpired() {
    return this.closeDate != null && this.closeDate.isBefore(LocalDateTime.now());
  }

  public void validateOwner(User user) {
    if (!this.publisherUser.getId().equals(user.getId())) {
      throw new ForbiddenException("The user is not the owner of the auction");
    }
  }

  public AuctionOffer findOfferById(String offerId) {
    return this.offers.stream()
        .filter(offer -> offer.getId().equals(offerId))
        .findFirst().orElseThrow(() -> new UnprocessableException("Offer not found"));
  }

  public void changeBestOffer(AuctionOffer offer) {
    if (offer.getStatus().equals(AuctionOfferStatus.REJECTED)) {
      throw new UnprocessableException("Cannot set a rejected offer as the best offer");
    }
    this.setBestOffer(offer);
  }
}
