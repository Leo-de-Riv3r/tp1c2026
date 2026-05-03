package com.tacs.tp1c2026.entities.exchange;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;
import com.tacs.tp1c2026.entities.user.User;
import lombok.Getter;
import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import com.tacs.tp1c2026.exceptions.MissingCardException;
import com.tacs.tp1c2026.exceptions.OfferAlreadyProcessedException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.ArrayList;
import java.util.List;


@TypeAlias("trade_proposal")
public class TradeProposal {

  @Id
  @Getter
  private Integer id;

  @DocumentReference
  @Getter
  private final List<Card> cards;

  @DocumentReference
  @Getter
  private final User proposerUser;

  private TradeProposalStatus status = TradeProposalStatus.PENDING;

  public TradeProposal(List<Card> cards, User proposerUser) {
    this.cards = new ArrayList<>(cards);
    this.proposerUser = proposerUser;
  }

  /**
   * Rechaza esta propuesta.
   */
  public void reject() {
    this.status = TradeProposalStatus.REJECTED;
  }

  /**
   * Acepta esta propuesta.
   */
  public void accept() {
    this.status = TradeProposalStatus.ACCEPTED;
  }

  /**
   * Verifica si la propuesta está pendiente.
   *
   * @return true si está pendiente
   */
  public boolean isPending() {
    return TradeProposalStatus.PENDING.equals(this.status);
  }

  /**
   * Valida que la propuesta esté pendiente.
   *
   * @throws OfferAlreadyProcessedException si la propuesta ya fue aceptada o rechazada
   */
  public void validatePending() throws OfferAlreadyProcessedException {
    if (!isPending()) {
      throw new OfferAlreadyProcessedException("La propuesta ya fue aceptada o rechazada");
    }
  }

  /**
   * Transfiere las figuritas ofrecidas al usuario destino.
   * Aumenta las repetidas del destino, elimina de faltantes si corresponde,
   * y reduce las repetidas del usuario que hizo la propuesta.
   */
  public void execute() throws MissingCardException, InsufficientCardException {
      for (Card s : this.cards){
        this.proposerUser.removeFromCollection(s.getId(), 1);
      }
  }

}
