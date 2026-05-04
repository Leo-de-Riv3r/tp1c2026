package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionItem;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.auction.input.CancelAuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;
import com.tacs.tp1c2026.entities.dto.auction.input.CreationAuctionOfferDTO;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.mappers.CreateAuctionDTOMapper;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.AuctionRepository;
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
public class AuctionService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CardService cardService;
    private final AuctionRepository auctionRepository;
    private final PageableGenerator pageableGenerator;

    public AuctionService(UserRepository userRepository,
                          UserService userService,
                          CardService cardService,
                          AuctionRepository auctionRepository,
                          PageableGenerator pageableGenerator) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.cardService = cardService;
        this.auctionRepository = auctionRepository;
        this.pageableGenerator = pageableGenerator;
    }

    /**
     * Crea una nueva subasta sobre una figurita de la colección del usuario.
     * Compromete una unidad de la figurita en la colección.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public Auction createAuction(String userId, CreateAuctionDTO dto) throws InsufficientCardException, MissingCardException, UserNotFoundException, NotFoundException {
        User user = this.userService.getById(userId);
        Card card = this.cardService.getById(dto.cardId());
        CollectionCard item = user.findCollectionItem(card.getId())
            .orElseThrow(() -> new MissingCardException("User does not have card " + card.getId()));
        item.commit(1);
        List<AuctionCondition> conditions = CreateAuctionDTOMapper.toDomainConditions(dto.conditions());
        Auction auction = new Auction(user, card, dto.auctionDurationHours(), conditions);
        Auction saved = this.auctionRepository.save(auction);
        this.userRepository.save(user);
        return saved;
    }

    /**
     * Registra una oferta sobre una subasta activa.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void createAuctionOffer(String userId, CreationAuctionOfferDTO dto) throws InsufficientCardException, MissingCardException, NotFoundException, UserNotFoundException {
        User proposer = this.userService.getById(userId);
        Auction auction = this.getAuctionById(dto.auctionId());

        if (Objects.equals(auction.getPublisherUser().getId(), proposer.getId())) {
            throw new ConflictException("El usuario no puede ofertar en su propia subasta");
        }
        if (auction.isExpired()) {
            throw new ConflictException("La subasta ya venció");
        }

        List<AuctionItem> offerItems = new ArrayList<>();
        for (CreationAuctionOfferDTO.Item it : dto.items()) {
            Card card = this.cardService.getById(it.cardId());
            CollectionCard item = proposer.findCollectionItem(card.getId())
                .orElseThrow(() -> new MissingCardException("User does not have card " + card.getId()));
            item.commit(it.amount());
            offerItems.add(new AuctionItem(card, it.amount()));
        }
        AuctionOffer offer = new AuctionOffer(proposer, offerItems);
        auction.addOffer(offer);
        this.auctionRepository.save(auction);
        this.userRepository.save(proposer);
    }

    /**
     * Cancela una subasta activa. Libera el `compromisedCount` de las figuritas
     * involucradas (la del subastante y las ofrecidas en cada oferta pendiente).
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void cancelAuction(String userId, CancelAuctionDto dto) throws AuctionClosedException, NotFoundException, UserNotFoundException, ForbiddenException {
        User user = this.userService.getById(userId);
        Auction auction = this.getAuctionById(dto.getAuctionId());

        if (!Objects.equals(auction.getPublisherUser().getId(), user.getId())) {
            throw new ForbiddenException("El usuario no es el dueño de la subasta");
        }

        auction.cancel();
        this.auctionRepository.save(auction);

        // Libera la unidad comprometida del subastante
        user.findCollectionItem(auction.getCard().getId()).ifPresent(item -> item.release(1));
        this.userRepository.save(user);

        // Libera las unidades comprometidas de cada postor
        for (AuctionOffer offer : auction.getOffers()) {
            User bidder = offer.getBidder();
            for (AuctionItem oi : offer.getOfferedItems()) {
                bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
            }
            this.userRepository.save(bidder);
        }
    }

    /**
     * Búsqueda paginada de subastas activas con filtros (país, equipo, categoría, nombre).
     */
    public Page<Auction> searchActiveAuctions(Integer page, Integer perPage, SearchPublicationsFilters filters) {
        Pageable pageable = pageableGenerator.buildPageable(page, perPage, 10, null);
        return auctionRepository.searchWithFilters(filters, pageable);
    }

    /**
     * Subastas creadas por el usuario, paginadas y ordenadas por fecha de creación descendente.
     */
    public Page<Auction> getMyAuctions(String userId, Integer page, Integer perPage) {
        Pageable pageable = pageableGenerator.buildPageable(page, perPage, 10,
            Sort.by("creationDate").descending());
        return auctionRepository.findByPublisherUserId(userId, pageable);
    }

    /**
     * Marca al usuario como interesado en la subasta.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void addInterestedUser(String auctionId, String userId) throws NotFoundException, UserNotFoundException {
        User user = this.userService.getById(userId);
        Auction auction = this.getAuctionById(auctionId);
        auction.addInterestedUser(user);
        this.auctionRepository.save(auction);
    }

    public List<Auction> getAuctions() {
        return this.auctionRepository.findAll();
    }

    public Auction getAuctionById(String id) throws NotFoundException {
        return this.auctionRepository.findById(id).orElseThrow(() -> new NotFoundException("Auction not found"));
    }
}
