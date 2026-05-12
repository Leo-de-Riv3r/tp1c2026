package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionItem;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.auction.input.CancelAuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;
import com.tacs.tp1c2026.entities.dto.auction.input.CreationAuctionOfferDTO;
import com.tacs.tp1c2026.entities.dto.auction.output.UserBidDto;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.mappers.AuctionMapper;
import com.tacs.tp1c2026.entities.dto.mappers.CreateAuctionDTOMapper;
import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.utils.PageableGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CardService cardService;
    private final AuctionRepository auctionRepository;
    private final PageableGenerator pageableGenerator;
    private final ExchangeService exchangeService;
    @Autowired
    private AuctionMapper auctionMapper;
    public AuctionService(UserRepository userRepository,
                          UserService userService,
                          CardService cardService,
                          AuctionRepository auctionRepository,
                          PageableGenerator pageableGenerator,
                          ExchangeService exchangeService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.cardService = cardService;
        this.auctionRepository = auctionRepository;
        this.pageableGenerator = pageableGenerator;
        this.exchangeService = exchangeService;
    }

    /**
     * Crea una nueva subasta sobre una figurita de la colección del usuario.
     * Compromete una unidad de la figurita en la colección.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public Auction createAuction(String userId, CreateAuctionDTO dto) throws InsufficientCardException, MissingCardException, UserNotFoundException, NotFoundException {
        User user = this.userService.getById(userId);
        Card card = this.cardService.getById(dto.getCardId());
        CollectionCard item = user.findCollectionItem(card.getId())
            .orElseThrow(() -> new MissingCardException("User does not have card " + card.getId()));
        item.commit(1);
        List<AuctionCondition> conditions = CreateAuctionDTOMapper.toDomainConditions(dto.getConditions());
        Auction auction = new Auction(user, card, dto.getAuctionDurationHours(), conditions);
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
          this.userRepository.save(bidder);
        }
      }
    }

    /**
     * Búsqueda paginada de subastas activas con filtros (país, equipo, categoría, nombre).
     */
    public PaginationDtoOutput<AuctionDto> searchActiveAuctions(Integer page, Integer per_page, SearchPublicationsFilters filters) {
       Pageable pageable = pageableGenerator.buildPageable(
            page,
            per_page,
            20,
            null
        );

        Page<Auction> pageResult = auctionRepository.searchWithFilters(filters, pageable);

        List<AuctionDto> dtos = auctionMapper.mapAuctions(pageResult.getContent());
        return new PaginationDtoOutput<>(dtos, pageResult.getNumber() + 1, pageResult.getTotalPages());
    }

    /**
     * Subastas creadas por el usuario, paginadas y ordenadas por fecha de creación descendente.
     */
    public Page<Auction> getMyAuctions(String userId, Integer page, Integer per_page) {
      Pageable pageable = pageableGenerator.buildPageable(page, per_page, 10,
          Sort.by("createdDate").descending()
      );

      Page<Auction> pageResult = auctionRepository.findByPublisherUserId(userId, pageable);
      return pageResult;
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

    /**
     * Lista plana de las ofertas hechas por un usuario, con el contexto de la subasta.
     */
    public List<UserBidDto> getMyOffers(String userId) {
        List<Auction> auctions = this.auctionRepository.findByOffersBidderId(userId);
        List<UserBidDto> offers = auctions.stream()
            .flatMap(auction -> auction.getOffers().stream()
                .filter(offer -> Objects.equals(offer.getBidderId(), userId))
                .map(offer -> toUserBid(auction, offer)))
            .toList();
            return offers;
    }

    private UserBidDto toUserBid(Auction auction, AuctionOffer offer) {
        List<UserBidDto.OfferItemDto> items = offer.getOfferedItems() == null
            ? List.of()
            : offer.getOfferedItems().stream()
                .map(it -> new UserBidDto.OfferItemDto(
                    it.getCard() != null ? it.getCard().getId() : null,
                    it.getCard() != null ? it.getCard().getNumber() : null,
                    it.getCard() != null ? it.getCard().getDescription() : null,
                    it.getAmount()))
                .toList();
        return new UserBidDto(
            auction.getId(),
            auction.getCardNumber(),
            auction.getCardDescription(),
            auction.getCardCountry(),
            auction.getCardTeam(),
            auction.getPublisherUser() != null ? auction.getPublisherUser().getId() : null,
            auction.getPublisherName(),
            auction.getStatus(),
            auction.getCloseDate(),
            offer.getId(),
            items,
            offer.getStatus(),
            offer.getBidDate()
        );
    }

    public Auction getAuctionById(String id) throws NotFoundException {
        return this.auctionRepository.findById(id).orElseThrow(() -> new NotFoundException("Auction not found"));
    }

    public void setAuctionOfferAsBest(String auctionId, String offerId, String userId) {
      User user = userService.getById(userId);
      Auction auction = getAuctionById(auctionId);
      auction.validateOwner(user);
      //find offer by id
      AuctionOffer offer = auction.findOfferById(offerId);
      auction.changeBestOffer(offer);
      auctionRepository.save(auction);
    }

    //@Transactional
    public void rejectAuctionOffer(String auctionId, String offerId, String userId) {
      User user = userService.getById(userId);
      Auction auction = getAuctionById(auctionId);
      auction.validateOwner(user);
      //find offer by id
      AuctionOffer rejectedOffer = auction.findOfferById(offerId);
      rejectedOffer.cancel();
      auctionRepository.save(auction);
      User bidder = rejectedOffer.getBidder();
      //return cards to bidder
      for(AuctionItem oi : rejectedOffer.getOfferedItems()) {
        bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
      }
      userRepository.save(bidder);
    }

    /**
     * Cierra una subasta cuya `closeDate` ya pasó. Adjudica al `bestOffer` si existe,
     * o cancela y libera commits si no hay ofertas. Entry point del cronjob.
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void closeExpiredAuction(String auctionId) throws NotFoundException, AuctionClosedException, OfferAlreadyProcessedException, OfferNotFoundException {
        Auction auction = getAuctionById(auctionId);
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ConflictException("La subasta ya está cerrada");
        }

        AuctionOffer best = auction.getBestOffer();
        if (best != null) {
            awardAuctionTo(auction, best);
        } else {
            cancelEmptyAuction(auction);
        }

        auctionRepository.save(auction);
    }

    /**
     * El publisher acepta manualmente una oferta y cierra la subasta. Mismo flujo que
     * el cron pero el ganador lo elige el usuario (no requiere `bestOffer` previo).
     */
    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void acceptAuctionOffer(String auctionId, String offerId, String userId) throws UserNotFoundException, NotFoundException, AuctionClosedException, OfferAlreadyProcessedException, OfferNotFoundException, ForbiddenException {
        User reviewer = userService.getById(userId);
        Auction auction = getAuctionById(auctionId);
        auction.validateOwner(reviewer);
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ConflictException("La subasta ya está cerrada");
        }
        AuctionOffer offer = auction.findOfferById(offerId);
        awardAuctionTo(auction, offer);
        auctionRepository.save(auction);
    }

    private void awardAuctionTo(Auction auction, AuctionOffer winningOffer) throws AuctionClosedException, OfferAlreadyProcessedException, OfferNotFoundException {
        User publisher = auction.getPublisherUser();
        User winner = winningOffer.getBidder();
        Card publishedCard = auction.getCard();

        auction.acceptOffer(winningOffer);

        transferCard(publisher, winner, publishedCard, 1);
        for (AuctionItem oi : winningOffer.getOfferedItems()) {
            transferCard(winner, publisher, oi.getCard(), oi.getAmount());
        }

        for (AuctionOffer other : auction.getOffers()) {
            if (other == winningOffer) continue;
            if (other.getStatus() != AuctionOfferStatus.CANCELLED) {
                User bidder = other.getBidder();
                for (AuctionItem oi : other.getOfferedItems()) {
                    bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
                }
                userRepository.save(bidder);
                other.reject();
            }
        }

        publisher.incrementExchangesAmount();
        winner.incrementExchangesAmount();
        userRepository.save(winner);
        userRepository.save(publisher);

        List<Card> offeredCardsExpanded = winningOffer.getOfferedItems().stream()
            .flatMap(oi -> java.util.Collections.nCopies(oi.getAmount(), oi.getCard()).stream())
            .toList();
        exchangeService.createFromAcceptedAuction(auction.getId(), publisher, winner, publishedCard, offeredCardsExpanded);
    }

    private void cancelEmptyAuction(Auction auction) {
        User publisher = auction.getPublisherUser();
        Card publishedCard = auction.getCard();

        auction.cancel();
        publisher.findCollectionItem(publishedCard.getId()).ifPresent(item -> item.release(1));
        userRepository.save(publisher);

        for (AuctionOffer offer : auction.getOffers()) {
            User bidder = offer.getBidder();
            for (AuctionItem oi : offer.getOfferedItems()) {
                bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
            }
            userRepository.save(bidder);
        }
    }

    //@Transactional
    public void cancelOffer(String offerId, String userId, String auctionId) {
      User user = userService.getById(userId);
      Auction auction = getAuctionById(auctionId);
      if(auction.isExpired()) {
        throw new ConflictException("NO se puede cancelar una oferta de una subasta finalizada");
      }
      AuctionOffer offer = auction.findOfferById(offerId);
      offer.validateCreator(userId);
      offer.cancel();
      //return cards to  bidder
      User bidder = offer.getBidder();
      for(AuctionItem oi : offer.getOfferedItems()) {
        bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
      }
      userRepository.save(bidder);
      auctionRepository.save(auction);
    }
  /**
   * Cierra todas las subastas activas cuya fecha de cierre ya pasó. Devuelve cuántas se cerraron.
   */
    public int closeAllExpiredAuctions() {
        List<Auction> active = auctionRepository.findByStatus(AuctionStatus.ACTIVE);
        int closed = 0;
        for (Auction a : active) {
            if (!a.isExpired()) continue;
            try {
                closeExpiredAuction(a.getId());
                closed++;
            } catch (Exception ignored) {
                // Si falla una, seguimos con las demás
            }
        }
        return closed;
    }

    /**
     * Transfiere {@code amount} unidades de {@code card} desde {@code from} hacia {@code to}.
     * En `from` libera el compromise y decrementa quantity. En `to` incrementa quantity
     * (creando el subdocumento si no existía).
     */
    private void transferCard(User from, User to, Card card, int amount) {
        from.findCollectionItem(card.getId()).ifPresent(item -> {
            item.release(amount);
            try {
                item.decrement(amount);
            } catch (InsufficientCardException ignored) {
                // No debería pasar — el compromise garantiza disponibilidad
            }
        });

        to.findCollectionItem(card.getId()).ifPresentOrElse(
            existing -> existing.increment(amount),
            () -> to.addToCollection(buildCollectionCard(card, amount))
        );
    }

    private CollectionCard buildCollectionCard(Card card, int amount) {
        return CollectionCard.builder()
            .cardId(card.getId())
            .number(card.getNumber())
            .description(card.getDescription())
            .country(card.getCountry())
            .team(card.getTeam())
            .category(card.getCategory() == null ? null : card.getCategory().getValue())
            .quantity(amount)
            .compromisedCount(0)
            .acquisitionDate(LocalDate.now())
            .acquisitionOrigin("AUCTION")
            .build();
    }

}
