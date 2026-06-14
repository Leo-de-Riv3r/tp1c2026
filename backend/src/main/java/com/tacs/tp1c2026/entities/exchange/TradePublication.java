package com.tacs.tp1c2026.entities.exchange;


import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.Objects;

@Document(collection = "publications")
@TypeAlias("publication")
@Getter
public class TradePublication {

  @Id
  private String id;

  @Version
  private Long version;

  @DocumentReference
  private User publisherUser;

  // Snapshot of publisher (denormalized) — avoids join on read
  private String publisherName;
  private String publisherAvatarId;

  @DocumentReference
  private Card card;

  private String cardId;
  private Integer cardNumber;
  private String cardDescription;
  private String cardCountry;
  private String cardTeam;
  private Category cardCategory;

  /** Quantity offered when publishing. Immutable post-creation. */
  private Integer initialCount;

  /** Available quantity. Decremented when a proposal is accepted by its requestedCount. */
  private Integer remainingCount;

  private final LocalDateTime creationDate = LocalDateTime.now();

  private PublicationStatus status = PublicationStatus.ACTIVE;

  protected TradePublication() {}

  public TradePublication(User publisherUser, Card card, Integer quantity) {
    this.publisherUser = publisherUser;
    this.publisherName = publisherUser.getName();
    this.publisherAvatarId = publisherUser.getAvatarId();
    this.card = card;
    this.cardId = card.getId();
    this.cardNumber = card.getNumber();
    this.cardDescription = card.getDescription();
    this.cardCountry = card.getCountry();
    this.cardTeam = card.getTeam();
    this.cardCategory = card.getCategory();
    this.initialCount = quantity;
    this.remainingCount = quantity;
  }

  public boolean isActive() {
    return this.status == PublicationStatus.ACTIVE;
  }

  public boolean notCancelled() {
    return this.status != PublicationStatus.CANCELLED;
  }

  /**
   * Validates that the user is the owner of the publication.
   *
   * @throws ForbiddenException if the user is not the owner (HTTP 403)
   */
  public void validateOwner(User user) throws ForbiddenException {
    if (!Objects.equals(this.publisherUser.getId(), user.getId())) {
      throw new ForbiddenException("The user is not the owner of the publication");
    }
  }

  /**
   * Decrements {@code remainingCount} by {@code amount} (the amount requested in the accepted proposal).
   * If it reaches 0, marks the publication as FINALIZED. The cascade to cancel pending proposals
   * lives in the service.
   *
   * @throws ConflictException if {@code amount > remainingCount}
   */
  public void decrementRemaining(int amount) {
    if (amount > this.remainingCount) {
      throw new ConflictException("The proposal requests " + amount + " but only " + this.remainingCount + " remain");
    }
    this.remainingCount -= amount;
    if (this.remainingCount == 0) {
      this.status = PublicationStatus.FINALIZED;
    }
  }

  /** Marks the publication as CANCELLED. The cascade of pending proposals lives in the service. */
  public void cancel() {
    this.status = PublicationStatus.CANCELLED;
  }
}
