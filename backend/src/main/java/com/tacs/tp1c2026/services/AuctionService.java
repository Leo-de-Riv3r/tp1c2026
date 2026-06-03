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
import com.tacs.tp1c2026.events.UserInterestedInActionEvent;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.utils.PageableGenerator;
import org.springframework.beans.factory.annotation.Autowired;
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
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  @Autowired
  private NotificationService notificationService;
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
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public Auction createAuction(String userId, CreateAuctionDto dto) throws InsufficientCardException, MissingCardException, UserNotFoundException, NotFoundException {
        User user = this.userService.getById(userId);
        Card card = this.cardService.getById(dto.getCardId());
        CollectionCard item = user.findCollectionItem(card.getId())
            .orElseThrow(() -> new MissingCardException("User does not have card " + card.getId()));
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
     * Registra una oferta sobre una subasta activa.¿
     * @return la {@link AuctionOffer} recién creada (con id generado), para que el controller pueda exponerla al cliente en el response
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public AuctionOffer createAuctionOffer(String userId, CreationAuctionOfferDto dto) throws InsufficientCardException, MissingCardException, NotFoundException, UserNotFoundException {
        User proposer = this.userService.getById(userId);
        Auction auction = this.getAuctionById(dto.auctionId());

        if (Objects.equals(auction.getPublisherUser().getId(), proposer.getId())) {
            throw new ConflictException("El usuario no puede ofertar en su propia subasta");
        }
        if (auction.isExpired()) {
            throw new ConflictException("La subasta ya venció");
        }

        List<AuctionItem> offerItems = new ArrayList<>();
        for (CreationAuctionOfferDto.Item it : dto.items()) {
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
        notificationService.createUserNotification(
            auction.getPublisherUser(),
            NotificationType.AUCTION_OFFER_RECEIVED,
            auction.getId(),
            "Recibiste una nueva oferta en tu subasta de #" + auction.getCardNumber()
        );
        return offer;
    }

    /**
     * Cancela una subasta activa. Libera el `compromisedCount` de las figuritas
     * involucradas (la del subastante y las ofrecidas en cada oferta pendiente).
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
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

      // Libera las unidades comprometidas de cada postor y notifica
      // Re-fetch por bidderId: @DocumentReference no hidrata bien en subdocs embebidos
      for (AuctionOffer offer : auction.getOffers()) {
        User bidder = userRepository.findById(offer.getBidderId())
            .orElseThrow(() -> new NotFoundException("Bidder not found: " + offer.getBidderId()));
        for (AuctionItem oi : offer.getOfferedItems()) {
          bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
        }
        userRepository.save(bidder);
        notificationService.createUserNotification(
            bidder,
            NotificationType.AUCTION_CANCELLED,
            auction.getId(),
            "La subasta donde ofertaste fue cancelada."
        );
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
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void addInterestedUser(String auctionId, String userId) throws NotFoundException, UserNotFoundException {
        User user = this.userService.getById(userId);
        Auction auction = this.getAuctionById(auctionId);
        auction.addInterestedUser(user);
        this.auctionRepository.save(auction);

        eventPublisher.publishEvent(new UserInterestedInActionEvent(user, auction));

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
      // Re-fetch por bidderId: @DocumentReference no hidrata bien en subdocs embebidos
      User bidder = userRepository.findById(rejectedOffer.getBidderId())
          .orElseThrow(() -> new NotFoundException("Bidder not found: " + rejectedOffer.getBidderId()));
      //return cards to bidder
      for(AuctionItem oi : rejectedOffer.getOfferedItems()) {
        bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
      }
      userRepository.save(bidder);
      notificationService.createUserNotification(
          bidder,
          NotificationType.AUCTION_OFFER_REJECTED,
          auctionId,
          "Tu oferta en la subasta de #" + auction.getCardNumber() + " fue rechazada."
      );
    }

    /**
     * Cierra una subasta cuya `closeDate` ya pasó. Adjudica al `bestOffer` si existe,
     * o cancela y libera commits si no hay ofertas. Entry point del cronjob.
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
            cancelEmptyAuction(auction);
        }

        auctionRepository.save(auction);
    }

    /**
     * El publisher acepta manualmente una oferta y cierra la subasta. Mismo flujo que
     * el cron pero el ganador lo elige el usuario (no requiere `bestOffer` previo).
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
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
        // @DocumentReference no hidrata correctamente dentro de subdocumentos embebidos en arrays
        // (caso de AuctionOffer.bidder dentro de Auction.offers). Re-fetcheamos por id para
        // garantizar que la colección del usuario esté completa antes de modificarla.
        User publisher = userRepository.findById(auction.getPublisherUser().getId())
            .orElseThrow(() -> new NotFoundException("Publisher not found"));
        User winner = userRepository.findById(winningOffer.getBidderId())
            .orElseThrow(() -> new NotFoundException("Winner not found"));
        Card publishedCard = auction.getCard();

        auction.acceptOffer(winningOffer);

        transferCard(publisher, winner, publishedCard, 1);
        for (AuctionItem oi : winningOffer.getOfferedItems()) {
            transferCard(winner, publisher, oi.getCard(), oi.getAmount());
        }

        // Cache para evitar cargar el mismo usuario varias veces: si el mismo bidder tiene
        // múltiples ofertas perdedoras (o es el mismo que el winner/publisher), reusar la
        // instancia ya cargada evita conflictos de optimistic locking al guardar.
        java.util.Map<String, User> bidderCache = new java.util.HashMap<>();
        bidderCache.put(publisher.getId(), publisher);
        bidderCache.put(winner.getId(), winner);

        for (AuctionOffer other : auction.getOffers()) {
            // Comparar por id, no por referencia: Spring puede hidratar auction.bestOffer y
            // auction.offers[i] como instancias Java distintas del mismo offer lógico.
            if (Objects.equals(other.getId(), winningOffer.getId())) continue;
            if (other.getStatus() != AuctionOfferStatus.CANCELLED) {
                User bidder = bidderCache.computeIfAbsent(other.getBidderId(), id ->
                    userRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Bidder not found: " + id)));
                for (AuctionItem oi : other.getOfferedItems()) {
                    bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
                }
                other.reject();
                notificationService.createUserNotification(
                    bidder,
                    NotificationType.AUCTION_OFFER_REJECTED,
                    auction.getId(),
                    "Tu oferta en la subasta de #" + auction.getCardNumber() + " fue rechazada."
                );
            }
        }

        // Guardar bidders que no son winner ni publisher (éstos se guardan abajo)
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
            "Tu oferta en la subasta de #" + auction.getCardNumber() + " fue aceptada."
        );

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

        // Re-fetch por bidderId: @DocumentReference no hidrata bien en subdocs embebidos
        for (AuctionOffer offer : auction.getOffers()) {
            User bidder = userRepository.findById(offer.getBidderId())
                .orElseThrow(() -> new NotFoundException("Bidder not found: " + offer.getBidderId()));
            for (AuctionItem oi : offer.getOfferedItems()) {
                bidder.findCollectionItem(oi.getCard().getId()).ifPresent(item -> item.release(oi.getAmount()));
            }
            userRepository.save(bidder);
        }
    }

    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
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
      // Re-fetch por bidderId: @DocumentReference no hidrata bien en subdocs embebidos
      User bidder = userRepository.findById(offer.getBidderId())
          .orElseThrow(() -> new NotFoundException("Bidder not found: " + offer.getBidderId()));
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
