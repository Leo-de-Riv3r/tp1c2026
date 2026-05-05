package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradeProposalDTO;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProposalService {

    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final UserService userService;
    private final CardService cardService;
    private final PublicationService publicationService;
    private final ExchangeService exchangeService;

    public ProposalService(UserRepository userRepository,
                           PublicationRepository publicationRepository,
                           UserService userService,
                           CardService cardService,
                           PublicationService publicationService,
                           ExchangeService exchangeService) {
        this.userRepository = userRepository;
        this.publicationRepository = publicationRepository;
        this.userService = userService;
        this.cardService = cardService;
        this.publicationService = publicationService;
        this.exchangeService = exchangeService;
    }

    /**
     * Crea una propuesta sobre una publicación. Compromete las figuritas ofrecidas
     * por el proponente en su colección.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public TradeProposal createProposal(String userId, CreateTradeProposalDTO dto)
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
        publication.validateAvailableSlots();

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
        publication.addProposal(proposal);
        publicationRepository.save(publication);
        userRepository.save(proposer);
        return proposal;
    }

    /**
     * Encuentra una propuesta por id, navegando desde la publicación que la contiene.
     */
    public TradeProposal findProposal(String proposalId) throws NotFoundException {
        return publicationRepository.findAll().stream()
            .flatMap(p -> p.getProposals().stream())
            .filter(p -> Objects.equals(p.getId(), proposalId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Proposal not found: " + proposalId));
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
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void acceptProposal(String userId, String proposalId)
            throws UserNotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException, MissingCardException, InsufficientCardException {
        User reviewer = userService.getById(userId);
        TradeProposal proposal = findProposal(proposalId);
        TradePublication publication = publicationService.findPublication(proposal.getPublication().getId());
        publication.validateOwner(reviewer);

        // La entidad valida que requestedCount <= remainingCount, decrementa por M y maneja cascada.
        publication.acceptProposal(proposal);

        int requested = proposal.getRequestedCount();
        User publisher = publication.getPublisherUser();
        User proposer = proposal.getProposerUser();
        Card publishedCard = publication.getCard();
        List<Card> offeredCards = proposal.getCards();

        // Proponente cede las N offered cards (1 por aparición)
        for (Card c : offeredCards) {
            proposer.findCollectionItem(c.getId()).ifPresent(item -> {
                item.release(1);
                item.decrement(1);
            });
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
        // Publicante cede M unidades de la card publicada (release + decrement por M)
        publisher.findCollectionItem(publishedCard.getId()).ifPresent(item -> {
            item.release(requested);
            item.decrement(requested);
        });
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

        exchangeService.createFromAcceptedProposal(proposal.getId(), publisher, proposer, publishedCard, offeredCards);

        // Cascada: si la publi quedó FINALIZADA, las pendientes restantes ya fueron marcadas
        // como CANCELLED por la entity. Liberar su compromisedCount + persistir.
        for (TradeProposal cancelled : publication.getCancelledPendingsForRelease()) {
            if (Objects.equals(cancelled.getId(), proposal.getId())) continue;
            User otherProposer = cancelled.getProposerUser();
            for (Card c : cancelled.getCards()) {
                otherProposer.findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
            }
            userRepository.save(otherProposer);
        }

        userRepository.save(proposer);
        userRepository.save(publisher);
        publicationRepository.save(publication);
    }

    /**
     * Rechaza una propuesta. Solo libera el compromisedCount del proponente; no transfiere nada.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void rejectProposal(String userId, String proposalId)
            throws UserNotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException {
        User reviewer = userService.getById(userId);
        TradeProposal proposal = findProposal(proposalId);
        TradePublication publication = publicationService.findPublication(proposal.getPublication().getId());
        publication.validateOwner(reviewer);

        publication.rejectProposal(proposal);
        for (Card c : proposal.getCards()) {
            proposal.getProposerUser().findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
        }
        userRepository.save(proposal.getProposerUser());
        publicationRepository.save(publication);
    }

    /**
     * Cancela una propuesta del lado del proponente. Libera el compromisedCount.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
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
        TradePublication publication = publicationService.findPublication(proposal.getPublication().getId());
        userRepository.save(proposal.getProposerUser());
        publicationRepository.save(publication);
    }
}
