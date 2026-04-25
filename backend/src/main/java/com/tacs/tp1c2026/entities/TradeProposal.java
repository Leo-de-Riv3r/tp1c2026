package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.PropuestaYaProcesadaException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


@TypeAlias("propuestaIntercambio")
public class TradeProposal {

  @Id
  private Integer id;

  @DocumentReference
  private final List<Sticker> stickers;

  @DocumentReference
  private final User proposerUser;

  private TradeProposalStatus status = TradeProposalStatus.PENDING;

  public TradeProposal( List<Sticker> stickers, User proposerUser) {
    this.stickers = new ArrayList<>(stickers);
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
   *
   * @param destinationUser usuario que recibe las figuritas
   */
  public void execute(User destinationUser) throws FiguritaNoEncontradaException{
      for (Sticker s : this.stickers){
        this.proposerUser.removeSticker(s);
        destinationUser.addSticker(s,this.proposerUser);
      }
  }

}
