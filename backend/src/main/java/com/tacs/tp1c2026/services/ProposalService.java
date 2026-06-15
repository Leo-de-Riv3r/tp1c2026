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
    private final SettingsService settingsService;
    public ProposalService(UserRepository userRepository,
                           PublicationRepository publicationRepository,
                           ProposalRepository proposalRepository,
                           UserService userService,
                           CardService cardService,
                           PublicationService publicationService,
                           ExchangeService exchangeService, NotificationService notificationService,
                           SettingsService settingsService) {
        this.userRepository = userRepository;
        this.publicationRepository = publicationRepository;
        this.proposalRepository = proposalRepository;
        this.userService = userService;
        this.cardService = cardService;
        this.publicationService = publicationService;
        this.exchangeService = exchangeService;
      this.notificationService = notificationService;
        this.settingsService = settingsService;
    }

    /**
     * Crea una propuesta sobre una publicación. Compromete las figuritas ofrecidas en la colección del proponente.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public TradeProposal createProposal(String userId, CreateTradeProposalDto dto)
            throws NotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
        TradePublication publication = publicationService.findPublication(dto.publicationId());
        if (!publication.isActive()) {
            throw new ConflictException("La publicación no está activa");
        }

        Integer requested = dto.requestedCount();
        if (requested == null || requested < 1) {
            throw new ConflictException("requestedCount debe ser >= 1");
        }
        // No se puede pedir más figuritas de las que la publicación tiene disponibles.
        if (requested > publication.getRemainingCount()) {
            throw new ConflictException("La propuesta pide " + requested + " pero quedan " + publication.getRemainingCount() + " disponibles");
        }

        // Modelo marketplace: una publicación admite hasta N propuestas PENDIENTES (configurable
        // por el admin). Se permite sobre-suscripción: varias propuestas pueden competir por las
        // mismas unidades. La disponibilidad real se resuelve al aceptar (decrementRemaining) y la
        // cascada cancela las pendientes que ya no entran.
        //
        // SOFT CAP (a propósito): el count+insert no está serializado, así que dos creates
        // concurrentes en el borde (count == N-1) podrían terminar en N+1. Es un tope anti-spam,
        // no un invariante de escasez (esa se resuelve al aceptar, serializado por @Version +
        // @Retryable). Para hacerlo estricto habría que bumpear la @Version de la publicación acá
        // y dejar que el retry recuente. Diferido para revisar después.
        int maxPending = settingsService.getMaxPendingProposals();
        long currentPending = proposalRepository
            .findByPublicationIdAndStatus(publication.getId(), TradeProposalStatus.PENDING).size();
        if (currentPending >= maxPending) {
            throw new ConflictException("La publicación alcanzó el máximo de propuestas pendientes (" + maxPending + ")");
        }

        User proposer = userService.getById(userId);
        User publisher = publication.getPublisherUser();

        if (Objects.equals(proposer.getId(), publisher.getId())) {
            throw new ConflictException("No podés proponer sobre tu propia publicación");
        }

        List<Card> cards = new ArrayList<>();
        for (String cardId : dto.cardIds()) {
            cards.add(cardService.getById(cardId));
        }
        // Commit individual por ocurrencia — si el oferente ofrece 3x la misma cardId, se le suma 3
        // al compromisedCount de ese CollectionCard. La validación del invariante vive en commit().
        for (Card c : cards) {
            CollectionCard item = proposer.findCollectionItem(c.getId())
                .orElseThrow(() -> new MissingCardException("El user no tiene la figurita " + c.getId()));
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
            .orElseThrow(() -> new NotFoundException("No se encontró la propuesta: " + proposalId));
    }

    /**
     * Lista las propuestas relacionadas a un user.
     *
     * @param userId id del user participante
     * @param role   "proposer" → propuestas hechas por el user;
     *               "publisher" / "receiver" → propuestas recibidas (sobre las publicaciones del user)
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

    /**
     * Lista las propuestas sobre una publicación, filtradas por la visibilidad del caller:
     *  - el publisher ve todas las propuestas (para aceptar/rechazar).
     *  - cualquier otro user ve sólo sus propias propuestas sobre esa publicación.
     * Devuelve lista vacía si la publicación no existe.
     */
    public List<TradeProposal> findByPublicationIdForUser(String publicationId, String currentUserId) {
        List<TradeProposal> all = proposalRepository.findByPublicationId(publicationId);
        if (all.isEmpty()) return all;
        String publisherUserId = all.getFirst().getPublication().getPublisherUser().getId();
        if (Objects.equals(publisherUserId, currentUserId)) return all;
        return all.stream()
            .filter(p -> Objects.equals(p.getProposerUser().getId(), currentUserId))
            .toList();
    }

    /**
     * Acepta una propuesta. Ejecuta el flow bilateral completo:
     *  - el publisher cede M (= requestedCount) unidades de la figurita publicada.
     *  - el proponente cede N (= offeredCards.size()) unidades de las figuritas ofrecidas.
     *  - cada uno recibe las figuritas del otro en su colección (el proponente recibe M de la publicada).
     *  - incrementa el {@code exchangesAmount} de ambos.
     *  - crea el documento histórico {@link com.tacs.tp1c2026.entities.exchange.Exchange}.
     *  - decrementa el {@code remainingCount} de la publicación en M; si llega a 0, finaliza y
     *    cancela en cascada las propuestas pendientes (liberando también su compromisedCount).
     *
     * @return el id del {@link com.tacs.tp1c2026.entities.exchange.Exchange} creado, para que el
     * controller lo exponga en la response y el FE pueda redirigir al detalle sin un GET extra.
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

        // El proponente cede N figuritas ofrecidas (1 por ocurrencia).
        for (Card c : offeredCards) {
            proposer.releaseAndDecrement(c.getId(), 1);
        }
        // El publisher recibe N figuritas ofrecidas.
        for (Card c : offeredCards) {
            CollectionCard publisherItem = publisher.findCollectionItem(c.getId()).orElse(null);
            if (publisherItem != null) {
                publisherItem.increment(1);
            } else {
                publisher.addToCollection(CollectionCard.fromCatalog(c));
            }
        }
        // El publisher cede M unidades de la figurita publicada (release + decrement + remove si quantity=0).
        publisher.releaseAndDecrement(publishedCard.getId(), requested);
        // El proponente recibe M unidades de la figurita publicada.
        CollectionCard proposerItem = proposer.findCollectionItem(publishedCard.getId()).orElse(null);
        if (proposerItem != null) {
            proposerItem.increment(requested);
        } else {
            CollectionCard fresh = CollectionCard.fromCatalog(publishedCard);
            proposer.addToCollection(fresh);
            // fromCatalog inicializa quantity=1; suma las M-1 restantes si requested > 1.
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

        // Cascada marketplace: tras consumir unidades, cancelar (silenciosamente) las pendientes
        // que ya no entran porque piden más de lo que quedó disponible. Si remainingCount llegó a 0
        // (publicación FINALIZED), esto cancela todas las pendientes restantes.
        cancelUnsatisfiablePendingProposals(publication.getId(), publication.getRemainingCount(), proposal.getId());
        return exchange.getId();
    }

    /**
     * Rechaza una propuesta. Sólo libera el compromisedCount del proponente; no transfiere nada.
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
     * Cancela una propuesta desde el lado del proponente. Libera su compromisedCount.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void cancelProposal(String userId, String proposalId)
            throws NotFoundException, NotFoundException, OfferAlreadyProcessedException, ForbiddenException {
        TradeProposal proposal = findProposal(proposalId);
        if (!Objects.equals(proposal.getProposerUser().getId(), userId)) {
            throw new ForbiddenException("Sólo el proponente puede cancelar su propia propuesta");
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
     * Cancela (silenciosamente) las propuestas PENDIENTES de una publicación que ya no se pueden
     * satisfacer porque piden más unidades de las que quedan disponibles ({@code requestedCount >
     * remaining}), liberando su compromisedCount. Excluye opcionalmente una (la recién aceptada).
     * Con {@code remaining == 0} cancela todas las pendientes restantes (publicación FINALIZED).
     * No notifica: el rechazo explícito del publisher es el que notifica, no esta cascada.
     */
    public void cancelUnsatisfiablePendingProposals(String publicationId, int remaining, String exceptProposalId) {
        List<TradeProposal> pendings = proposalRepository
            .findByPublicationIdAndStatus(publicationId, TradeProposalStatus.PENDING);
        for (TradeProposal p : pendings) {
            if (exceptProposalId != null && Objects.equals(p.getId(), exceptProposalId)) continue;
            if (p.getRequestedCount() <= remaining) continue;
            p.cancel();
            User otherProposer = p.getProposerUser();
            for (Card c : p.getCards()) {
                otherProposer.findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
            }
            proposalRepository.save(p);
            userRepository.save(otherProposer);
        }
    }

    /**
     * Cancela todas las propuestas pendientes de una publicación (opcionalmente excluyendo una)
     * y libera el compromisedCount de cada proponente. Lo usa la cancelación manual de la publicación.
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
