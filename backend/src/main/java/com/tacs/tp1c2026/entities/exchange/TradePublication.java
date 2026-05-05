package com.tacs.tp1c2026.entities.exchange;


import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;
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

  /** Cantidad ofrecida al publicar. Inmutable post-creación. */
  private Integer initialCount;

  /** Cantidad disponible. Decrementa cuando se acepta una proposal por su requestedCount. */
  private Integer remainingCount;

  private final LocalDateTime creationDate = LocalDateTime.now();

  private PublicationStatus status = PublicationStatus.ACTIVE;

  private final List<TradeProposal> proposals = new ArrayList<>();

  protected TradePublication() {}

  public TradePublication(User publisherUser, Card card, Integer quantity) {
    this.publisherUser = publisherUser;
    this.card = card;
    this.cardNumber = card.getNumber();
    this.cardDescription = card.getDescription();
    this.cardCountry = card.getCountry();
    this.cardTeam = card.getTeam();
    this.cardCategory = card.getCategory();
    this.initialCount = quantity;
    this.remainingCount = quantity;
  }

  /**
   * Verifica si hay cupos disponibles para nuevas propuestas, considerando las pendientes
   * (que ya tienen reservado su requestedCount frente a remainingCount).
   */
  public boolean hasAvailableSlots() {
    int pendingRequested = this.proposals.stream()
        .filter(TradeProposal::isPending)
        .mapToInt(TradeProposal::getRequestedCount)
        .sum();
    return pendingRequested < this.remainingCount;
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
   * Acepta una propuesta de esta publicación. Decrementa {@code remainingCount} por
   * {@code proposal.requestedCount}. Si llega a 0, finaliza la publicación y marca como
   * {@code CANCELLED} las pendientes restantes (el service libera su {@code compromisedCount}).
   */
  public void acceptProposal(TradeProposal proposal)
      throws ProposalNotInPublicationException, OfferAlreadyProcessedException {
    validateProposalBelongsToPublication(proposal);
    proposal.validatePending();
    int requested = proposal.getRequestedCount();
    if (requested > this.remainingCount) {
      throw new ConflictException("La propuesta pide " + requested + " pero quedan " + this.remainingCount);
    }
    proposal.accept();
    this.remainingCount -= requested;
    if (this.remainingCount == 0) {
      this.status = PublicationStatus.FINALIZED;
      this.proposals.stream()
          .filter(TradeProposal::isPending)
          .forEach(TradeProposal::cancel);
    }
  }

  /**
   * Devuelve las propuestas que el service tiene que liberar (release de compromisedCount)
   * después de un cierre por cascada (auto-finalización o cancelación manual).
   */
  public List<TradeProposal> getCancelledPendingsForRelease() {
    return this.proposals.stream()
        .filter(p -> p.getStatus() == com.tacs.tp1c2026.entities.enums.TradeProposalStatus.CANCELLED)
        .toList();
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
