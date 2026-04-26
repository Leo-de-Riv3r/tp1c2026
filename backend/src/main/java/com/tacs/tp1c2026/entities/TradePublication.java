package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.exceptions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import lombok.Getter;
import org.springframework.data.annotation.Id;


public class TradePublication {

  @Id
  @Getter
  private Integer id;

  @Getter
  private final Integer amountTraded;

  private final LocalDateTime createdAt = LocalDateTime.now();

  private PublicationStatus status = PublicationStatus.ACTIVE;

  private final List<TradeProposal> proposals = new ArrayList<>();

  private Feedback feedback;

  public TradePublication(
          Integer amountTraded
  ) {
    this.amountTraded = amountTraded;
  }


  /**
   * Verifica si hay cupos disponibles para nuevas propuestas.
   *
   * @return true si hay cupos disponibles
   */
  public boolean hasAvailableSlots() {
    long pendingProposals = this.proposals.stream()
        .filter(TradeProposal::isPending)
        .count();
    return pendingProposals < amountTraded;
  }

  public boolean isActive(){
    return this.status == PublicationStatus.ACTIVE;
  }

  /**
   * Valida si hay cupos disponibles para nuevas propuestas.
   *
   * @throws CuposAgotadosException si no hay cupos disponibles
   */
  public void validateAvailableSlots() throws CuposAgotadosException {
    if (!hasAvailableSlots()) {
      throw new CuposAgotadosException("Ya no hay cupos para nuevas propuestas");
    }
  }


  /**
   * Valida que una propuesta corresponda a esta publicación.
   *
   * @param proposal propuesta a validar
   * @throws PropuestaNoCorrespondeException si la propuesta no corresponde a esta publicación
   */
  public void validateProposalBelongsToPublication(TradeProposal proposal) throws PropuestaNoCorrespondeException {
    if (this.proposals.contains(proposal)) {
      throw new PropuestaNoCorrespondeException("La publicacion no corresponde a la propuesta");
    }
  }

  /**
   * Rechaza una propuesta de esta publicación.
   * Valida que la propuesta corresponda a esta publicación y que esté pendiente.
   *
   * @param proposal propuesta a rechazar
   * @throws PropuestaNoCorrespondeException si la propuesta no corresponde
   * @throws UsuarioNoAutorizadoException si el usuario no es el dueño
   * @throws PropuestaYaProcesadaException si la propuesta ya fue procesada
   */
  public void rejectProposal(TradeProposal proposal)
      throws PropuestaNoCorrespondeException, PropuestaYaProcesadaException {
    validateProposalBelongsToPublication(proposal);
    proposal.validatePending();
    proposal.reject();
  }

  /**
   * Acepta una propuesta de esta publicación y ejecuta el intercambio.
   * Reduce el stock, transfiere figuritas y rechaza las demás propuestas.
   *
   * @param proposal propuesta a aceptar
   * @throws PropuestaNoCorrespondeException si la propuesta no corresponde
   * @throws UsuarioNoAutorizadoException si el usuario no es el dueño
   * @throws PropuestaYaProcesadaException si la propuesta ya fue procesada
   */
  public void acceptProposal(TradeProposal proposal)
      throws PropuestaNoCorrespondeException, PropuestaYaProcesadaException {
    validateProposalBelongsToPublication(proposal);
    proposal.validatePending();
    this.status = PublicationStatus.FINALIZED;
    this.proposals.stream()
        .filter(otherProposal -> !otherProposal.equals(proposal))
        .filter(TradeProposal::isPending)
        .forEach(TradeProposal::reject);
  }

  /**
   * Agrega una propuesta a esta publicación.
   *
   * @param proposal propuesta a agregar
   */
  public void addProposal(TradeProposal proposal) {
    this.proposals.add(proposal);
  }

  public void addFeedback(Feedback feedback){
    this.feedback = feedback;
  }

  public boolean notCancelled() {
    return this.status != PublicationStatus.CANCELLED;
  }

  public void createProposal(User user, List<Sticker> stickers) throws InsufficientStickerException {
      try {
          for (Sticker sticker : stickers) {
              user.checkSufficientTradedSticker(sticker);
          }
      } catch (MissingStickerException e) {
          throw new InsufficientStickerException(e.getMessage());
      }

      for (Sticker sticker : stickers) {
          user.removeTradedSticker(sticker);
      }
      this.proposals.add(new TradeProposal(stickers, user));
  }
}
