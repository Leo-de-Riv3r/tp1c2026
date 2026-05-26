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
     * Crea una propuesta sobre una publicación. Compromete las figuritas ofrecidas
     * por el proponente en su colección.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public TradeProposal createProposal(String userId, CreateTradeProposalDto dto)
            throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException, NoAvailableSlotsException {
        TradePublication publication = publicationService.findPublication(dto.publicationId());
        if (!publication.isActive()) {
            throw new ConflictException("La publicación no está activa");
        }

        Integer requested = dto.requestedCount();
        if (requested == null || requested < 1) {
            throw new ConflictException("requestedCount debe ser >= 1");
        }
        if (requested > publication.getRemainingCount()) {
            throw new ConflictException("La propuesta pide " + requested + " pero quedan " + publication.getRemainingCount());
        }

        // Cupos: la suma de requestedCount de las pendientes no puede igualar/superar remainingCount.
        int pendingRequested = proposalRepository
            .findByPublicationIdAndStatus(publication.getId(), TradeProposalStatus.PENDING)
            .stream()
            .mapToInt(TradeProposal::getRequestedCount)
            .sum();
        if (pendingRequested >= publication.getRemainingCount()) {
            throw new NoAvailableSlotsException("Ya no hay cupos para nuevas propuestas");
        }

        User proposer = userService.getById(userId);
        User publisher = publication.getPublisherUser();

        if (Objects.equals(proposer.getId(), publisher.getId())) {
            throw new ConflictException("El usuario no puede proponer sobre su propia publicación");
        }

        List<Card> cards = new ArrayList<>();
        for (String cardId : dto.cardIds()) {
            cards.add(cardService.getById(cardId));
        }
        // Commit individual por aparición — si el bidder ofrece 3x el mismo cardId, suma 3 al
        // compromisedCount de esa CollectionCard. La validación del invariante vive en commit().
        for (Card c : cards) {
            CollectionCard item = proposer.findCollectionItem(c.getId())
                .orElseThrow(() -> new MissingCardException("User does not have card " + c.getId()));
            item.commit(1);
        }

        TradeProposal proposal = new TradeProposal(publication, cards, requested, proposer, publisher);
        TradeProposal saved = proposalRepository.save(proposal);  // autogenera id
        userRepository.save(proposer);
        notificationService.createNotification(
            publisher,
            NotificationType.TRADE_PROPOSAL_RECEIVED,
            saved.getId(),
            "Recibiste una nueva propuesta de " + proposer.getName()
        );
        return saved;
    }

    public TradeProposal findProposal(String proposalId) throws NotFoundException {
        return proposalRepository.findById(proposalId)
            .orElseThrow(() -> new NotFoundException("Proposal not found: " + proposalId));
    }

    /**
     * Lista propuestas relacionadas con un usuario.
     *
     * @param userId id del usuario participante
     * @param role   "proposer" → propuestas hechas por el usuario;
     *               "publisher" / "receiver" → propuestas recibidas (sobre publicaciones del usuario)
     * @param status filtro opcional por estado
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
     * Acepta una propuesta. Ejecuta el flujo bilateral completo:
     *  - publicante cede M (= requestedCount) unidades del card publicado
     *  - proponente cede N (= offeredCards.size()) unidades de las offered cards
     *  - cada uno recibe lo del otro lado en su colección (con M de la card publicada para el proponente)
     *  - incrementa exchangesAmount en ambos
     *  - crea el documento Exchange histórico
     *  - decrementa remainingCount de la publicación por M; si llega a 0, finaliza y cancela
     *    en cascada las pendientes (liberando su compromisedCount también).
     */
    /**
     * @return el id del {@link com.tacs.tp1c2026.entities.exchange.Exchange} creado, para que el controller pueda exponerlo en el response y el FE redirija al detalle sin GET extra
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public String acceptProposal(String userId, String proposalId)
            throws UserNotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException, MissingCardException, InsufficientCardException {
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

        // Proponente cede las N offered cards (1 por aparición)
        for (Card c : offeredCards) {
            proposer.releaseAndDecrement(c.getId(), 1);
        }
        // Publicante recibe las N offered cards
        for (Card c : offeredCards) {
            CollectionCard publisherItem = publisher.findCollectionItem(c.getId()).orElse(null);
            if (publisherItem != null) {
                publisherItem.increment(1);
            } else {
                publisher.addToCollection(CollectionCard.fromCatalog(c));
            }
        }
        // Publicante cede M unidades de la card publicada (release + decrement + remove si quantity=0)
        publisher.releaseAndDecrement(publishedCard.getId(), requested);
        // Proponente recibe M unidades de la card publicada
        CollectionCard proposerItem = proposer.findCollectionItem(publishedCard.getId()).orElse(null);
        if (proposerItem != null) {
            proposerItem.increment(requested);
        } else {
            CollectionCard fresh = CollectionCard.fromCatalog(publishedCard);
            proposer.addToCollection(fresh);
            // fromCatalog inicializa quantity=1; sumamos las M-1 restantes si requested > 1
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

        notificationService.createNotification(
            proposer,
            NotificationType.TRADE_PROPOSAL_ACCEPTED,
            proposal.getId(),
            "Tu propuesta fue aceptada. Revisá tus intercambios."
        );

        // Cascada: si la publi quedó FINALIZADA, cancelar pendientes restantes y liberar
        // su compromisedCount.
        if (publication.getStatus() == PublicationStatus.FINALIZED) {
            cancelPendingProposalsAndRelease(publication.getId(), proposal.getId());
        }
        return exchange.getId();
    }

    /**
     * Rechaza una propuesta. Solo libera el compromisedCount del proponente; no transfiere nada.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void rejectProposal(String userId, String proposalId)
            throws UserNotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException {
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
        notificationService.createNotification(
            proposer,
            NotificationType.TRADE_PROPOSAL_REJECTED,
            proposal.getId(),
            "Tu propuesta fue rechazada."
        );
    }

    /**
     * Cancela una propuesta del lado del proponente. Libera el compromisedCount.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void cancelProposal(String userId, String proposalId)
            throws UserNotFoundException, NotFoundException, OfferAlreadyProcessedException, ForbiddenException {
        TradeProposal proposal = findProposal(proposalId);
        if (!Objects.equals(proposal.getProposerUser().getId(), userId)) {
            throw new ForbiddenException("Solo el proponente puede cancelar la propuesta");
        }
        proposal.validatePending();
        proposal.cancel();
        for (Card c : proposal.getCards()) {
            proposal.getProposerUser().findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
        }
        proposalRepository.save(proposal);
        userRepository.save(proposal.getProposerUser());
    }

    /**
     * Cancela todas las pendientes de una publicación (excluyendo opcionalmente una) y libera
     * el compromisedCount de cada proponente. Usado por el accept en cascada (FINALIZED) y por
     * el cancel manual de la publicación.
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
