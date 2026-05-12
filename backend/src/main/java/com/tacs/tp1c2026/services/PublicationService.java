package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradePublicationDto;
import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.ProposalRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.utils.PageableGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicationService {

    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final ProposalRepository proposalRepository;
    private final UserService userService;
    private final CardService cardService;
    private final PageableGenerator pageableGenerator;

    public PublicationService(UserRepository userRepository,
                              PublicationRepository publicationRepository,
                              ProposalRepository proposalRepository,
                              UserService userService,
                              CardService cardService,
                              PageableGenerator pageableGenerator) {
        this.userRepository = userRepository;
        this.publicationRepository = publicationRepository;
        this.proposalRepository = proposalRepository;
        this.userService = userService;
        this.cardService = cardService;
        this.pageableGenerator = pageableGenerator;
    }

    /**
     * Crea una publicación de N unidades de una figurita. Compromete N unidades en la
     * colección del publicante.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public TradePublication createPublication(String userId, CreateTradePublicationDto dto)
            throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
        User user = this.userService.getById(userId);
        Card card = this.cardService.getById(dto.cardId());
        CollectionCard item = user.findCollectionItem(card.getId())
            .orElseThrow(() -> new MissingCardException("User does not have card " + card.getId()));
        item.commit(dto.quantity());
        TradePublication publication = new TradePublication(user, card, dto.quantity());
        TradePublication saved = this.publicationRepository.save(publication);
        this.userRepository.save(user);
        return saved;
    }

    /**
     * Cancela una publicación. Libera todas las unidades comprometidas restantes
     * (de la publicada y de las propuestas pendientes que se cancelan en cascada).
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void cancelPublication(String userId, String publicationId)
            throws UserNotFoundException, NotFoundException, ForbiddenException {
        User user = this.userService.getById(userId);
        TradePublication publication = this.findPublication(publicationId);
        publication.validateOwner(user);

        if (!publication.isActive()) {
            throw new ConflictException("Solo se puede cancelar una publicación activa");
        }

        Integer remaining = publication.getRemainingCount();
        publication.cancel();
        this.publicationRepository.save(publication);

        // Libera las unidades comprometidas restantes en el publicante
        user.findCollectionItem(publication.getCard().getId()).ifPresent(item -> item.release(remaining));
        this.userRepository.save(user);

        // Cascada: cancelar pendientes y liberar su compromisedCount
        List<TradeProposal> pendings = proposalRepository
            .findByPublicationIdAndStatus(publicationId, TradeProposalStatus.PENDING);
        for (TradeProposal p : pendings) {
            p.cancel();
            User proposer = p.getProposerUser();
            for (Card c : p.getCards()) {
                proposer.findCollectionItem(c.getId()).ifPresent(item -> item.release(1));
            }
            proposalRepository.save(p);
            userRepository.save(proposer);
        }
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

    public TradePublication findPublication(String publicationId) throws NotFoundException {
        return this.publicationRepository.findById(publicationId)
            .orElseThrow(() -> new NotFoundException("Publication not found: " + publicationId));
    }
}
