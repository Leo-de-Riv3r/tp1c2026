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

  // Snapshot del publisher (desnormalizado) — evita el join en lectura
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

  /** Cantidad ofrecida al publicar. Inmutable luego de la creación. */
  private Integer initialCount;

  /** Cantidad disponible. Se decrementa cuando se acepta una propuesta, por su requestedCount. */
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
   * Valida que el usuario sea el dueño de la publicación.
   *
   * @throws ForbiddenException si el usuario no es el dueño (HTTP 403)
   */
  public void validateOwner(User user) throws ForbiddenException {
    if (!Objects.equals(this.publisherUser.getId(), user.getId())) {
      throw new ForbiddenException("Operación no permitida. No es dueño de la publicación");
    }
  }

  /**
   * Decrementa {@code remainingCount} en {@code amount} (la cantidad solicitada en la propuesta aceptada).
   * Si llega a 0, marca la publicación como FINALIZED. La cascada para cancelar las propuestas pendientes
   * vive en el service.
   *
   * @throws ConflictException si {@code amount > remainingCount}
   */
  public void decrementRemaining(int amount) {
    if (amount > this.remainingCount) {
      throw new ConflictException("La propuesta pide " + amount + " pero solo quedan " + this.remainingCount);
    }
    this.remainingCount -= amount;
    if (this.remainingCount == 0) {
      this.status = PublicationStatus.FINALIZED;
    }
  }

  /** Marca la publicación como CANCELLED. La cascada de las propuestas pendientes vive en el service. */
  public void cancel() {
    this.status = PublicationStatus.CANCELLED;
  }
}
