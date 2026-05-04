package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.feedback.input.NewFeedbackDto;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradeProposalDTO;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradePublicationDto;
import com.tacs.tp1c2026.entities.dto.trade.input.ReviewProposalDto;
import com.tacs.tp1c2026.entities.enums.ReviewAction;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.entities.exchange.embedded.Feedback;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.utils.PageableGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PublicationService {

    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final UserService userService;
    private final CardService cardService;
    private final PageableGenerator pageableGenerator;

    public PublicationService(UserRepository userRepository,
                              PublicationRepository publicationRepository,
                              UserService userService,
                              CardService cardService,
                              PageableGenerator pageableGenerator) {
        this.userRepository = userRepository;
        this.publicationRepository = publicationRepository;
        this.userService = userService;
        this.cardService = cardService;
        this.pageableGenerator = pageableGenerator;
    }

    /**
     * Crea una publicación de N unidades de una figurita. Compromete N unidades en la
     * colección del publicante.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public TradePublication createPublication(String userId, CreateTradePublicationDto dto) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
        User user = this.userService.getById(userId);
        Card card = this.cardService.getById(dto.cardId());
        CollectionCard item = user.findCollectionItem(card.getId())
            .orElseThrow(() -> new MissingCardException("User does not have card " + card.getId()));
        item.commit(dto.amount());
        TradePublication publication = new TradePublication(user, card, dto.amount());
        TradePublication saved = this.publicationRepository.save(publication);
        this.userRepository.save(user);
        return saved;
    }

    /**
     * Crea una propuesta sobre una publicación. Compromete las figuritas ofrecidas
     * por el proponente.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public TradeProposal createTradeProposalForPublication(String userId, CreateTradeProposalDTO dto) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException, NoAvailableSlotsException {
        TradePublication publication = this.findPublication(dto.publicationId());
        if (!publication.isActive()) {
            throw new ConflictException("La publicación no está activa");
        }
        publication.validateAvailableSlots();

        User proposer = this.userService.getById(userId);
        User receiver = publication.getPublisherUser();

        if (Objects.equals(proposer.getId(), receiver.getId())) {
            throw new ConflictException("El usuario no puede proponer sobre su propia publicación");
        }

        List<Card> cards = new ArrayList<>();
        for (String cardId : dto.cardIds()) {
            cards.add(this.cardService.getById(cardId));
        }
        for (Card c : cards) {
            CollectionCard item = proposer.findCollectionItem(c.getId())
                .orElseThrow(() -> new MissingCardException("User does not have card " + c.getId()));
            item.commit(1);
        }

        TradeProposal proposal = new TradeProposal(publication, cards, proposer, receiver);
        publication.addProposal(proposal);
        this.publicationRepository.save(publication);
        this.userRepository.save(proposer);
        return proposal;
    }

    /**
     * Acepta o rechaza una propuesta. Si acepta y la publicación queda con
     * `quantity = 0`, la cierra automáticamente.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void reviewProposal(String userId, ReviewProposalDto dto) throws UserNotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException, MissingCardException, InsufficientCardException {
        User reviewer = this.userService.getById(userId);
        TradePublication publication = this.findPublication(dto.getPublicationId());
        publication.validateOwner(reviewer);

        TradeProposal proposal = publication.getProposals().stream()
            .filter(p -> Objects.equals(p.getId(), dto.getProposalId()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Propuesta no encontrada"));

        if (dto.getAction() == ReviewAction.REJECT) {
            publication.rejectProposal(proposal);
            // Liberar compromisedCount del proponente
            for (Card c : proposal.getCards()) {
                proposal.getProposerUser().findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
            }
            this.userRepository.save(proposal.getProposerUser());
        } else {
            publication.acceptProposal(proposal);
            // Transferencia: el proponente entrega cards (decrement quantity, release compromise),
            // el publicante las recibe; el publicante entrega 1 de la publicada y el proponente la recibe.
            User proposer = proposal.getProposerUser();
            User publisher = publication.getPublisherUser();
            Card published = publication.getCard();

            for (Card c : proposal.getCards()) {
                proposer.findCollectionItem(c.getId()).ifPresent(item -> {
                    item.release(1);
                    item.decrement(1);
                });
                CollectionCard publisherItem = publisher.findCollectionItem(c.getId()).orElse(null);
                if (publisherItem != null) {
                    publisherItem.increment(1);
                } else {
                    publisher.addToCollection(CollectionCard.fromCatalog(c));
                }
            }
            publisher.findCollectionItem(published.getId()).ifPresent(item -> {
                item.release(1);
                item.decrement(1);
            });
            CollectionCard proposerItem = proposer.findCollectionItem(published.getId()).orElse(null);
            if (proposerItem != null) {
                proposerItem.increment(1);
            } else {
                proposer.addToCollection(CollectionCard.fromCatalog(published));
            }

            this.userRepository.save(proposer);
            this.userRepository.save(publisher);
        }

        this.publicationRepository.save(publication);
    }

    /**
     * Cancela una publicación. Libera todas las unidades comprometidas restantes
     * (de la publicada y de las propuestas pendientes que se cancelan en cascada).
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void cancelPublication(String userId, String publicationId) throws UserNotFoundException, NotFoundException, ForbiddenException {
        User user = this.userService.getById(userId);
        TradePublication publication = this.findPublication(publicationId);
        publication.validateOwner(user);

        Integer remaining = publication.getQuantity();
        publication.cancel();
        this.publicationRepository.save(publication);

        // Libera las unidades comprometidas restantes en el publicante
        user.findCollectionItem(publication.getCard().getId()).ifPresent(item -> item.release(remaining));
        this.userRepository.save(user);

        // Libera las cards comprometidas de los proponentes pendientes
        publication.getProposals().stream()
            .filter(p -> p.getStatus() == com.tacs.tp1c2026.entities.enums.TradeProposalStatus.CANCELLED)
            .forEach(p -> {
                User proposer = p.getProposerUser();
                for (Card c : p.getCards()) {
                    proposer.findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
                }
                this.userRepository.save(proposer);
            });
    }

    /**
     * Agrega un feedback a una publicación finalizada.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void addFeedback(String userId, NewFeedbackDto dto) throws UserNotFoundException, NotFoundException {
        User reviewer = this.userService.getById(userId);
        TradePublication publication = this.findPublication(dto.getPublicationId());
        Feedback feedback = Feedback.builder()
            .reviewer(reviewer)
            .score(dto.getRating())
            .comment(dto.getCommentary())
            .build();
        publication.addFeedback(feedback);
        this.publicationRepository.save(publication);
    }

    /**
     * Búsqueda paginada de publicaciones activas con filtros.
     */
    public Page<TradePublication> searchActivePublications(Integer page, Integer perPage, SearchPublicationsFilters filters) {
        Pageable pageable = pageableGenerator.buildPageable(page, perPage, 10, null);
        return publicationRepository.searchWithFilters(filters, pageable);
    }

    /**
     * Publicaciones del usuario, paginadas y ordenadas por fecha descendente.
     */
    public Page<TradePublication> getMyPublications(String userId, Integer page, Integer perPage) {
        Pageable pageable = pageableGenerator.buildPageable(page, perPage, 10,
            Sort.by("creationDate").descending());
        return publicationRepository.findByPublisherUserId(userId, pageable);
    }

    private TradePublication findPublication(String publicationId) throws NotFoundException {
        return this.publicationRepository.findById(publicationId)
            .orElseThrow(() -> new NotFoundException("Publication not found: " + publicationId));
    }

}
