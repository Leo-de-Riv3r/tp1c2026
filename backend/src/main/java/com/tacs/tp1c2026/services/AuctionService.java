package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionItem;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.auction.input.CancelAuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreationAuctionOfferDto;
import com.tacs.tp1c2026.entities.dto.auction.output.UserBidDto;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.mappers.AuctionMapper;
import com.tacs.tp1c2026.entities.dto.mappers.CreateAuctionDtoMapper;
import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.events.AuctionCreatedEvent;
import com.tacs.tp1c2026.events.CardAvailableEvent;
import com.tacs.tp1c2026.events.UserInterestedInAuctionEvent;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.utils.PageableGenerator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final AuctionMapper auctionMapper;
    public AuctionService(UserRepository userRepository,
                          UserService userService,
                          CardService cardService,
                          AuctionRepository auctionRepository,
                          PageableGenerator pageableGenerator,
                          ExchangeService exchangeService,
                          ApplicationEventPublisher eventPublisher,
                          NotificationService notificationService,
                          AuctionMapper auctionMapper) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.cardService = cardService;
        this.auctionRepository = auctionRepository;
        this.pageableGenerator = pageableGenerator;
        this.exchangeService = exchangeService;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.auctionMapper = auctionMapper;
    }

    /**
     * Crea una nueva subasta sobre una figurita de la colección del user.
     * Compromete una unidad de la figurita en la colección.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public Auction createAuction(String userId, CreateAuctionDto dto) throws InsufficientCardException, MissingCardException, NotFoundException, NotFoundException {
        User user = this.userService.getById(userId);
        Card card = this.cardService.getById(dto.getCardId());
        CollectionCard item = user.findCollectionItem(card.getId())
            .orElseThrow(() -> new MissingCardException("El user no tiene la figurita " + card.getId()));
        item.commit(1);
        List<AuctionCondition> conditions = CreateAuctionDtoMapper.toDomainConditions(dto.getConditions());
        Auction auction = new Auction(user, card, dto.getAuctionDurationHours(), conditions);
        Auction saved = this.auctionRepository.save(auction);
        this.userRepository.save(user);

      eventPublisher.publishEvent(new CardAvailableEvent(
          saved.getCard().getId(),
          saved.getId(),
          "AUCTION"
      ));

      eventPublisher.publishEvent(new AuctionCreatedEvent(
          auction
      ));

        return saved;
    }

    /**
     * Registra una oferta sobre una subasta activa.
     * @return la {@link AuctionOffer} recién creada (con id autogenerado), para que el controller la exponga en la response.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public AuctionOffer createAuctionOffer(String userId, String auctionId, CreationAuctionOfferDto dto) throws InsufficientCardException, MissingCardException, NotFoundException, NotFoundException {
        User proposer = this.userService.getById(userId);
        Auction auction = this.getAuctionById(auctionId);

        if (Objects.equals(auction.getPublisherUser().getId(), proposer.getId())) {
            throw new ConflictException("No podés ofertar en tu propia subasta");
        }
        if (auction.isExpired()) {
            throw new ConflictException("La subasta ya expiró");
        }

        List<AuctionItem> offerItems = new ArrayList<>();
        for (CreationAuctionOfferDto.Item it : dto.items()) {
            Card card = this.cardService.getById(it.cardId());
            CollectionCard item = proposer.findCollectionItem(card.getId())
                .orElseThrow(() -> new MissingCardException("El user no tiene la figurita " + card.getId()));
            item.commit(it.amount());
            offerItems.add(new AuctionItem(card, it.amount()));
        }
        AuctionOffer offer = new AuctionOffer(proposer, offerItems);
        auction.addOffer(offer);
        this.auctionRepository.save(auction);
        this.userRepository.save(proposer);
        notificationService.createUserNotification(
            auction.getPublisherUser(),
            NotificationType.AUCTION_OFFER_RECEIVED,
            auction.getId(),
            "Recibiste una nueva oferta en tu subasta de " + auction.getCardId() + " - " + auction.getCardDescription()
        );
        return offer;
    }

    /**
     * Cancela una subasta activa. Libera el `compromisedCount` de las figuritas involucradas
     * (la del subastador y las ofrecidas en cada oferta pendiente).
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void cancelAuction(String userId, CancelAuctionDto dto) throws AuctionClosedException, NotFoundException, NotFoundException, ForbiddenException {
      User user = this.userService.getById(userId);
      Auction auction = this.getAuctionById(dto.getAuctionId());

      if (!Objects.equals(auction.getPublisherUser().getId(), user.getId())) {
        throw new ForbiddenException("El user no es dueño de la subasta");
      }

      List<User> bidders = auction.getOffers().stream()
          .filter(AuctionOffer::isPending)
          .map(o -> userRepository.findById(o.getBidderId())
              .orElseThrow(() -> new NotFoundException("No se encontró el oferente: " +o.getBidderId())))
          .toList();

      auction.cancel(user, bidders);
      this.auctionRepository.save(auction);
      this.userRepository.save(user);
      bidders.forEach(this.userRepository::save);

      for (AuctionOffer offer : auction.getOffers()) {
        User bidder = userRepository.findById(offer.getBidderId())
            .orElseThrow(() -> new NotFoundException("No se encontró el oferente: " +offer.getBidderId()));
        for (AuctionItem oi : offer.getOfferedItems()) {
          bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
        }
        userRepository.save(bidder);
        notificationService.createUserNotification(
            bidder,
            NotificationType.AUCTION_CANCELLED,
            auction.getId(),
            "Una subasta de " + auction.getCardId() + " - " + auction.getCardDescription() + " en la que ofertaste fue cancelada"
        );
      }
    }

    /**
     * Búsqueda paginada de subastas activas con filtros (country, team, category, name).
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
     * Subastas creadas por el user, paginadas y ordenadas por fecha de creación descendente.
     */
    public Page<Auction> getMyAuctions(String userId, Integer page, Integer per_page) {
      Pageable pageable = pageableGenerator.buildPageable(page, per_page, 10,
          Sort.by("createdDate").descending()
      );

      Page<Auction> pageResult = auctionRepository.findByPublisherUserId(userId, pageable);
      return pageResult;
    }

    /**
     * Marca al user como interesado en la subasta.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void addInterestedUser(String auctionId, String userId) throws NotFoundException, NotFoundException {
        User user = this.userService.getById(userId);
        Auction auction = this.getAuctionById(auctionId);
        auction.addInterestedUser(user);
        this.auctionRepository.save(auction);

        eventPublisher.publishEvent(new UserInterestedInAuctionEvent(user, auction));

    }

    public List<Auction> getAuctions() {
        return this.auctionRepository.findAll();
    }

    /**
     * Lista plana de ofertas hechas por un user, con el contexto de la subasta.
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
        return this.auctionRepository.findById(id).orElseThrow(() -> new NotFoundException("Subasta no encontrada"));
    }

    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void setAuctionOfferAsBest(String auctionId, String offerId, String userId) {
      User user = userService.getById(userId);
      Auction auction = getAuctionById(auctionId);
      auction.validateOwner(user);
      //find offer by id
      AuctionOffer offer = auction.findOfferById(offerId);
      auction.changeBestOffer(offer);
      auctionRepository.save(auction);
    }

    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void rejectAuctionOffer(String auctionId, String offerId, String userId) {
      User user = userService.getById(userId);
      Auction auction = getAuctionById(auctionId);
      auction.validateOwner(user);
      //find offer by id
      AuctionOffer rejectedOffer = auction.findOfferById(offerId);
      rejectedOffer.cancel();
      auctionRepository.save(auction);
      // Re-fetch by bidderId: @DocumentReference does not hydrate properly inside embedded subdocs
      User bidder = userRepository.findById(rejectedOffer.getBidderId())
          .orElseThrow(() -> new NotFoundException("No se encontró el oferente: " +rejectedOffer.getBidderId()));
      //return cards to bidder
      for(AuctionItem oi : rejectedOffer.getOfferedItems()) {
        bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
      }
      userRepository.save(bidder);
      notificationService.createUserNotification(
          bidder,
          NotificationType.AUCTION_OFFER_REJECTED,
          auctionId,
                    "Tu oferta en la subasta de " + auction.getCardId() + " - " + auction.getCardDescription() + " fue rechazada"
      );
    }

    /**
     * Cierra una subasta cuyo `closeDate` ya pasó. Adjudica a la `bestOffer` si existe;
     * si no hay ofertas, cancela y libera commits. Entry point del cron job.
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void closeExpiredAuction(String auctionId) throws NotFoundException, AuctionClosedException, OfferAlreadyProcessedException, OfferNotFoundException {
        Auction auction = getAuctionById(auctionId);
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ConflictException("La subasta ya está cerrada");
        }

        AuctionOffer best = auction.getBestOffer();
        if (best != null) {
            awardAuctionTo(auction, best);
        } else {
            User publisher = auction.getPublisherUser();
            List<User> bidders = auction.getOffers().stream()
                .filter(AuctionOffer::isPending)
                .map(o -> userRepository.findById(o.getBidderId())
                    .orElseThrow(() -> new NotFoundException("No se encontró el oferente: " +o.getBidderId())))
                .toList();
            auction.cancel(publisher, bidders);
            userRepository.save(publisher);
            bidders.forEach(this.userRepository::save);
        }

        auctionRepository.save(auction);
    }

    /**
     * El publisher acepta manualmente una oferta y cierra la subasta. Mismo flow que
     * el cron job, pero el ganador lo elige el user (no requiere `bestOffer` previa).
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void acceptAuctionOffer(String auctionId, String offerId, String userId) throws NotFoundException, NotFoundException, AuctionClosedException, OfferAlreadyProcessedException, OfferNotFoundException, ForbiddenException {
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
        // @DocumentReference does not hydrate correctly inside embedded subdocuments in arrays
        // (case of AuctionOffer.bidder within Auction.offers). Re-fetch by id to
        // ensure the user's collection is complete before modifying it.
        User publisher = userRepository.findById(auction.getPublisherUser().getId())
            .orElseThrow(() -> new NotFoundException("No se encontró el publicante"));
        User winner = userRepository.findById(winningOffer.getBidderId())
            .orElseThrow(() -> new NotFoundException("No se encontró al ganador"));
        Card publishedCard = auction.getCard();

        auction.acceptOffer(winningOffer);

        transferCard(publisher, winner, publishedCard, 1);
        for (AuctionItem oi : winningOffer.getOfferedItems()) {
            transferCard(winner, publisher, oi.getCard(), oi.getAmount());
        }

        // Cache to avoid loading the same user multiple times: if the same bidder has
        // multiple losing offers (or is the same as the winner/publisher), reusing the
        // already loaded instance avoids optimistic locking conflicts when saving.
        java.util.Map<String, User> bidderCache = new java.util.HashMap<>();
        bidderCache.put(publisher.getId(), publisher);
        bidderCache.put(winner.getId(), winner);

        for (AuctionOffer other : auction.getOffers()) {
            // Compare by id, not by reference: Spring may hydrate auction.bestOffer and
            // auction.offers[i] as distinct Java instances of the same logical offer.
            if (Objects.equals(other.getId(), winningOffer.getId())) continue;
            if (other.getStatus() != AuctionOfferStatus.CANCELLED) {
                User bidder = bidderCache.computeIfAbsent(other.getBidderId(), id ->
                    userRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("No se encontró el oferente: " +id)));
                for (AuctionItem oi : other.getOfferedItems()) {
                    bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
                }
                other.reject();
                notificationService.createUserNotification(
                    bidder,
                    NotificationType.AUCTION_OFFER_REJECTED,
                    auction.getId(),
          "Tu oferta en la subasta de " + auction.getCardId() + " - " + auction.getCardDescription() + " fue rechazada"
                );
            }
        }

        // Save bidders that are neither winner nor publisher (those are saved below)
        for (java.util.Map.Entry<String, User> entry : bidderCache.entrySet()) {
            if (!entry.getKey().equals(winner.getId()) && !entry.getKey().equals(publisher.getId())) {
                userRepository.save(entry.getValue());
            }
        }

        publisher.incrementExchangesAmount();
        winner.incrementExchangesAmount();
        userRepository.save(winner);
        userRepository.save(publisher);
        notificationService.createUserNotification(
            winner,
            NotificationType.AUCTION_OFFER_ACCEPTED,
            auction.getId(),
            "¡Tu oferta en la subasta de " + auction.getCardId() + " - " + auction.getCardDescription() + " fue aceptada!"
        );

        List<Card> offeredCardsExpanded = winningOffer.getOfferedItems().stream()
            .flatMap(oi -> java.util.Collections.nCopies(oi.getAmount(), oi.getCard()).stream())
            .toList();
        exchangeService.createFromAcceptedAuction(auction.getId(), publisher, winner, publishedCard, offeredCardsExpanded);
    }

    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void cancelOffer(String offerId, String userId, String auctionId) {
      User user = userService.getById(userId);
      Auction auction = getAuctionById(auctionId);
      if(auction.isExpired()) {
        throw new ConflictException("No se puede cancelar una oferta sobre una subasta finalizada");
      }
      AuctionOffer offer = auction.findOfferById(offerId);
      offer.validateCreator(userId);
      offer.cancel();
      //return cards to  bidder
      // Re-fetch by bidderId: @DocumentReference does not hydrate properly inside embedded subdocs
      User bidder = userRepository.findById(offer.getBidderId())
          .orElseThrow(() -> new NotFoundException("No se encontró el oferente: " +offer.getBidderId()));
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
                // If one fails, continue with the rest
            }
        }
        return closed;
    }

    /**
     * Transfiere {@code amount} unidades de {@code card} desde {@code from} hacia {@code to}.
     * En `from` libera el compromiso y decrementa la cantidad. En `to` incrementa la cantidad
     * (creando el subdocumento si no existía).
     */
    private void transferCard(User from, User to, Card card, int amount) {
        from.releaseAndDecrement(card.getId(), amount);

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
