package com.tacs.tp1c2026.entities.exchange;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "proposals")
@TypeAlias("trade_proposal")
@Getter
public class TradeProposal {

  @Id
  private String id;

  @DocumentReference
  private TradePublication publication;

  @DocumentReference
  private User proposerUser;

  @DocumentReference
  private User receiver;

  @DocumentReference
  private List<Card> cards;

  /**
   * Cantidad de unidades del card publicado que pide el bidder.
   * Tiene que cumplir 1 ≤ requestedCount ≤ publication.remainingCount al momento de crearse.
   */
  private Integer requestedCount;

  private TradeProposalStatus status = TradeProposalStatus.PENDING;

  private LocalDateTime creationDate = LocalDateTime.now();

  @Version // 🛡️ El escudo definitivo contra mutaciones concurrentes
  private Long version;

  protected TradeProposal() {}

  public TradeProposal(TradePublication publication, List<Card> cards, Integer requestedCount, User proposerUser, User receiver) {
    this.publication = publication;
    this.cards = new ArrayList<>(cards);
    this.requestedCount = requestedCount;
    this.proposerUser = proposerUser;
    this.receiver = receiver;
  }

  public void reject() {
    this.status = TradeProposalStatus.REJECTED;
  }

  public void accept() {
    this.status = TradeProposalStatus.ACCEPTED;
  }

  public void cancel() {
    this.status = TradeProposalStatus.CANCELLED;
  }

  public boolean isPending() {
    return TradeProposalStatus.PENDING.equals(this.status);
  }

  public void validatePending() throws ConflictException {
    if (!isPending()) {
      throw new ConflictException("La propuesta ya fue aceptada o rechazada previamente");
    }
  }

  public void validateOwner(String userId) {
    if (!Objects.equals(this.proposerUser.getId(), userId)) {
      throw new ForbiddenException("Operación no permitida. No es dueño de la propuesta");
    }
  }
}
