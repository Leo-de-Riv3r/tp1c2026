package com.tacs.tp1c2026.entities.exchange;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.MissingStickerException;
import com.tacs.tp1c2026.exceptions.PropuestaYaProcesadaException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.ArrayList;
import java.util.List;


@TypeAlias("propuestaIntercambio")
public class TradeProposal {

  @Id
  private Integer id;

  @DocumentReference
  private final List<Card> cards;

  @DocumentReference
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
   * @throws PropuestaYaProcesadaException si la propuesta ya fue aceptada o rechazada
   */
  public void validatePending() throws PropuestaYaProcesadaException {
    if (!isPending()) {
      throw new PropuestaYaProcesadaException("La propuesta ya fue aceptada o rechazada");
    }
  }

  /**
   * Transfiere las figuritas ofrecidas al usuario destino.
   * Aumenta las repetidas del destino, elimina de faltantes si corresponde,
   * y reduce las repetidas del usuario que hizo la propuesta.
   */
  public void execute() throws MissingStickerException {
      for (Card s : this.cards){
        this.proposerUser.removeTradedSticker(s);
      }
  }

}
