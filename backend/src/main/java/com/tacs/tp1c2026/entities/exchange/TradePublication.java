package com.tacs.tp1c2026.entities.exchange;


import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.exchange.embedded.Feedback;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.NoAvailableSlotsException;
import com.tacs.tp1c2026.exceptions.OfferAlreadyProcessedException;
import com.tacs.tp1c2026.exceptions.ProposalNotInPublicationException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "publications")
@TypeAlias("publication")
@Getter
public class TradePublication {

  @Id
  private String id;

  @DocumentReference
  private User publisherUser;

  @DocumentReference
  private Card card;

  private Integer cardNumber;
  private String cardDescription;
  private String cardCountry;
  private String cardTeam;
  private Category cardCategory;

  private Integer quantity;

  private final LocalDateTime creationDate = LocalDateTime.now();

  private PublicationStatus status = PublicationStatus.ACTIVE;

  private final List<TradeProposal> proposals = new ArrayList<>();

  private final List<Feedback> feedbacks = new ArrayList<>();

  public TradePublication(User publisherUser, Card card, Integer quantity) {
    this.publisherUser = publisherUser;
    this.card = card;
    this.cardNumber = card.getNumber();
    this.cardDescription = card.getDescription();
    this.cardCountry = card.getCountry();
    this.cardTeam = card.getTeam();
    this.cardCategory = card.getCategory();
    this.quantity = quantity;
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
    return pendingProposals < this.quantity;
  }

  public boolean isActive() {
    return this.status == PublicationStatus.ACTIVE;
  }

  /**
   * Valida si hay cupos disponibles para nuevas propuestas.
   *
   * @throws NoAvailableSlotsException si no hay cupos disponibles
   */
  public void validateAvailableSlots() throws NoAvailableSlotsException {
    if (!hasAvailableSlots()) {
      throw new NoAvailableSlotsException("Ya no hay cupos para nuevas propuestas");
    }
  }

  /**
   * Valida que el usuario sea el dueño de la publicación.
   *
   * @param user usuario a validar
   * @throws UnauthorizedException si el usuario no es el dueño
   */
  public void validateOwner(User user) throws UnauthorizedException {
    if (!Objects.equals(this.publisherUser.getId(), user.getId())) {
      throw new UnauthorizedException("El usuario no es el dueño de la publicación");
    }
  }

  /**
   * Valida que una propuesta corresponda a esta publicación.
   */
  public void validateProposalBelongsToPublication(TradeProposal proposal) throws ProposalNotInPublicationException {
    if (!Objects.equals(proposal.getPublication().getId(), this.id)) {
      throw new ProposalNotInPublicationException("La propuesta no corresponde a esta publicación");
    }
  }

  /**
   * Rechaza una propuesta de esta publicación.
   */
  public void rejectProposal(TradeProposal proposal)
      throws ProposalNotInPublicationException, OfferAlreadyProcessedException {
    validateProposalBelongsToPublication(proposal);
    proposal.validatePending();
    proposal.reject();
  }

  /**
   * Acepta una propuesta de esta publicación. Decrementa el cupo y, si llega a 0,
   * cierra la publicación.
   */
  public void acceptProposal(TradeProposal proposal)
      throws ProposalNotInPublicationException, OfferAlreadyProcessedException {
    validateProposalBelongsToPublication(proposal);
    proposal.validatePending();
    proposal.accept();
    this.quantity--;
    if (this.quantity == 0) {
      this.status = PublicationStatus.FINALIZED;
      this.proposals.stream()
          .filter(TradeProposal::isPending)
          .forEach(TradeProposal::reject);
    }
  }

  public void addProposal(TradeProposal proposal) {
    this.proposals.add(proposal);
  }

  public void removeProposal(String proposalId) {
    TradeProposal proposal = this.proposals.stream()
        .filter(p -> Objects.equals(p.getId(), proposalId))
        .findFirst()
        .orElseThrow(() -> new ProposalNotInPublicationException("Propuesta no encontrada"));
    if (!proposal.isPending()) {
      throw new ConflictException("No se puede eliminar una propuesta procesada");
    }
    this.proposals.removeIf(p -> Objects.equals(p.getId(), proposalId));
  }

  public void addFeedback(Feedback feedback) {
    this.feedbacks.add(feedback);
  }

  public void cancel() {
    this.status = PublicationStatus.CANCELLED;
    this.proposals.stream()
        .filter(TradeProposal::isPending)
        .forEach(TradeProposal::cancel);
  }

  public boolean notCancelled() {
    return this.status != PublicationStatus.CANCELLED;
  }
}
