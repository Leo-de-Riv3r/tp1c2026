package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradeProposalDto;
import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.ProposalRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProposalService {

    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final ProposalRepository proposalRepository;
    private final UserService userService;
    private final CardService cardService;
    private final PublicationService publicationService;
    private final ExchangeService exchangeService;
    private final NotificationService notificationService;
    public ProposalService(UserRepository userRepository,
                           PublicationRepository publicationRepository,
                           ProposalRepository proposalRepository,
                           UserService userService,
                           CardService cardService,
                           PublicationService publicationService,
                           ExchangeService exchangeService, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.publicationRepository = publicationRepository;
        this.proposalRepository = proposalRepository;
        this.userService = userService;
        this.cardService = cardService;
        this.publicationService = publicationService;
        this.exchangeService = exchangeService;
      this.notificationService = notificationService;
    }

    /**
     * Creates a proposal on a publication. Commits the offered cards in the proposer's collection.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public TradeProposal createProposal(String userId, CreateTradeProposalDto dto)
            throws NotFoundException, NotFoundException, InsufficientCardException, MissingCardException, NoAvailableSlotsException {
        TradePublication publication = publicationService.findPublication(dto.publicationId());
        if (!publication.isActive()) {
            throw new ConflictException("The publication is not active");
        }

        Integer requested = dto.requestedCount();
        if (requested == null || requested < 1) {
            throw new ConflictException("requestedCount debe ser >= 1");
        }
        if (requested > publication.getRemainingCount()) {
            throw new ConflictException("The proposal requests " + requested + " but only " + publication.getRemainingCount() + " remain");
        }

        // Atomic reservation: checks remainingCount - pendingCount >= requested and increments
        // pendingCount in a single findAndModify. Serializes concurrent requests without needing
        // multi-document transactions or replica set.
        if (!publicationRepository.tryReserveSlots(publication.getId(), requested)) {
            throw new NoAvailableSlotsException("No more slots available for new proposals");
        }

        User proposer = userService.getById(userId);
        User publisher = publication.getPublisherUser();

        if (Objects.equals(proposer.getId(), publisher.getId())) {
            throw new ConflictException("The user cannot propose on their own publication");
        }

        List<Card> cards = new ArrayList<>();
        for (String cardId : dto.cardIds()) {
            cards.add(cardService.getById(cardId));
        }
        // Individual commit per occurrence — if the bidder offers 3x the same cardId, adds 3 to
        // that CollectionCard's compromisedCount. The invariant validation lives in commit().
        for (Card c : cards) {
            CollectionCard item = proposer.findCollectionItem(c.getId())
                .orElseThrow(() -> new MissingCardException("User does not have card " + c.getId()));
            item.commit(1);
        }

        TradeProposal proposal = new TradeProposal(publication, cards, requested, proposer, publisher);
        TradeProposal saved = proposalRepository.save(proposal);  // autogenera id
        userRepository.save(proposer);
        notificationService.createUserNotification(
            publisher,
            NotificationType.TRADE_PROPOSAL_RECEIVED,
            saved.getId(),
            "Recibiste una propuesta nueva de " + proposer.getName() + "."
        );
        return saved;
    }

    public TradeProposal findProposal(String proposalId) throws NotFoundException {
        return proposalRepository.findById(proposalId)
            .orElseThrow(() -> new NotFoundException("Proposal not found: " + proposalId));
    }

    /**
     * Lists proposals related to a user.
     *
     * @param userId id of the participating user
     * @param role   "proposer" → proposals made by the user;
     *               "publisher" / "receiver" → proposals received (on the user's publications)
     * @param status optional filter by status
     */
    public List<TradeProposal> searchProposals(String userId, String role, TradeProposalStatus status) {
        List<TradeProposal> base;
        if ("publisher".equalsIgnoreCase(role) || "receiver".equalsIgnoreCase(role)) {
            base = proposalRepository.findByReceiverId(userId);
        } else {
            base = proposalRepository.findByProposerUserId(userId);
        }
        if (status != null) {
            return base.stream().filter(p -> p.getStatus() == status).toList();
        }
        return base;
    }

    public List<TradeProposal> findByPublicationId(String publicationId) {
        return proposalRepository.findByPublicationId(publicationId);
    }

    /**
     * Accepts a proposal. Executes the full bilateral flow:
     *  - publisher gives M (= requestedCount) units of the published card
     *  - proposer gives N (= offeredCards.size()) units of the offered cards
     *  - each receives the other's cards in their collection (proposer gets M of the published card)
     *  - increments exchangesAmount for both
     *  - creates the historical Exchange document
     *  - decrements publication's remainingCount by M; if it reaches 0, finalizes and cascades
     *    to cancel pending proposals (releasing their compromisedCount as well).
     */
    /**
     * @return the id of the created {@link com.tacs.tp1c2026.entities.exchange.Exchange}, so the controller can expose it in the response and the FE can redirect to the detail without an extra GET
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public String acceptProposal(String userId, String proposalId)
            throws NotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException, MissingCardException, InsufficientCardException {
        User reviewer = userService.getById(userId);
        TradeProposal proposal = findProposal(proposalId);
        TradePublication publication = publicationService.findPublication(proposal.getPublication().getId());
        publication.validateOwner(reviewer);
        proposal.validatePending();

        int requested = proposal.getRequestedCount();
        publication.decrementRemaining(requested);
        proposal.accept();

        User publisher = publication.getPublisherUser();
        User proposer = proposal.getProposerUser();
        Card publishedCard = publication.getCard();
        List<Card> offeredCards = proposal.getCards();

        // Proposer gives away N offered cards (1 per occurrence)
        for (Card c : offeredCards) {
            proposer.releaseAndDecrement(c.getId(), 1);
        }
        // Publisher receives N offered cards
        for (Card c : offeredCards) {
            CollectionCard publisherItem = publisher.findCollectionItem(c.getId()).orElse(null);
            if (publisherItem != null) {
                publisherItem.increment(1);
            } else {
                publisher.addToCollection(CollectionCard.fromCatalog(c));
            }
        }
        // Publisher gives away M units of the published card (release + decrement + remove if quantity=0)
        publisher.releaseAndDecrement(publishedCard.getId(), requested);
        // Proponente recibe M unidades de la card publicada
        CollectionCard proposerItem = proposer.findCollectionItem(publishedCard.getId()).orElse(null);
        if (proposerItem != null) {
            proposerItem.increment(requested);
        } else {
            CollectionCard fresh = CollectionCard.fromCatalog(publishedCard);
            proposer.addToCollection(fresh);
            // fromCatalog initializes quantity=1; add the remaining M-1 if requested > 1
            if (requested > 1) {
                proposer.findCollectionItem(publishedCard.getId()).ifPresent(item -> item.increment(requested - 1));
            }
        }

        publisher.incrementExchangesAmount();
        proposer.incrementExchangesAmount();

        com.tacs.tp1c2026.entities.exchange.Exchange exchange =
            exchangeService.createFromAcceptedProposal(proposal.getId(), publisher, proposer, publishedCard, offeredCards);

        proposalRepository.save(proposal);
        userRepository.save(proposer);
        userRepository.save(publisher);
        publicationRepository.save(publication);

        notificationService.createUserNotification(
            proposer,
            NotificationType.TRADE_PROPOSAL_ACCEPTED,
            proposal.getId(),
            "¡Tu propuesta fue aceptada! Revisá tus intercambios."
        );

        // Cascade: if the publication is FINALIZED, cancel remaining pending proposals and release
        // their compromisedCount.
        if (publication.getStatus() == PublicationStatus.FINALIZED) {
            cancelPendingProposalsAndRelease(publication.getId(), proposal.getId());
        }
        return exchange.getId();
    }

    /**
     * Rejects a proposal. Only releases the proposer's compromisedCount; does not transfer anything.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void rejectProposal(String userId, String proposalId)
            throws NotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException {
        User reviewer = userService.getById(userId);
        TradeProposal proposal = findProposal(proposalId);
        TradePublication publication = publicationService.findPublication(proposal.getPublication().getId());
        publication.validateOwner(reviewer);
        proposal.validatePending();

        proposal.reject();
        User proposer = proposal.getProposerUser();
        for (Card c : proposal.getCards()) {
            proposer.findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
        }
        proposalRepository.save(proposal);
        userRepository.save(proposer);
        notificationService.createUserNotification(
            proposer,
            NotificationType.TRADE_PROPOSAL_REJECTED,
            proposal.getId(),
            "Tu propuesta fue rechazada."
        );
    }

    /**
     * Cancels a proposal from the proposer's side. Releases the compromisedCount.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void cancelProposal(String userId, String proposalId)
            throws NotFoundException, NotFoundException, OfferAlreadyProcessedException, ForbiddenException {
        TradeProposal proposal = findProposal(proposalId);
        if (!Objects.equals(proposal.getProposerUser().getId(), userId)) {
            throw new ForbiddenException("Only the proposer can cancel the proposal");
        }
        proposal.validatePending();
        proposal.cancel();
        for (Card c : proposal.getCards()) {
            proposal.getProposerUser().findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
        }
        proposalRepository.save(proposal);
        userRepository.save(proposal.getProposerUser());
        publicationRepository.releaseSlots(proposal.getPublication().getId(), proposal.getRequestedCount());
    }

    /**
     * Cancels all pending proposals of a publication (optionally excluding one) and releases
     * each proposer's compromisedCount. Used by the cascade accept (FINALIZED) and by
     * manual publication cancel.
     */
    public void cancelPendingProposalsAndRelease(String publicationId, String exceptProposalId) {
        List<TradeProposal> pendings = proposalRepository
            .findByPublicationIdAndStatus(publicationId, TradeProposalStatus.PENDING);
        for (TradeProposal p : pendings) {
            if (exceptProposalId != null && Objects.equals(p.getId(), exceptProposalId)) continue;
            p.cancel();
            User otherProposer = p.getProposerUser();
            for (Card c : p.getCards()) {
                otherProposer.findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
            }
            proposalRepository.save(p);
            userRepository.save(otherProposer);
        }
    }
}
