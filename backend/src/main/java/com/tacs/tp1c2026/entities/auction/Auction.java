package com.tacs.tp1c2026.entities.auction;

import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.CardType;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.UnprocessableException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Document(collection = "auctions")
@Getter
@TypeAlias("auction")
public class Auction {

  @Id
  private String id;

  @Version
  private Long version;

  @DocumentReference
  private Card card;
  private String cardId;
  private Integer cardNumber;
  private String cardDescription;
  private String cardCountry;
  private String cardTeam;
  private Category cardCategory;
  private CardType cardType;

  @Setter
  @DocumentReference
  private User publisherUser;

  // Snapshot del publisher (desnormalizado) — evita el join en lectura
  private String publisherName;
  private String publisherAvatarId;

  private LocalDateTime creationDate;

  private LocalDateTime closeDate;

  private List<AuctionCondition> conditions = new ArrayList<>();

  private AuctionStatus status = AuctionStatus.ACTIVE;

  @Setter
  private AuctionOffer bestOffer;

  private List<AuctionOffer> offers = new ArrayList<>();

  @DocumentReference
  private List<User> interestedUsers = new ArrayList<>();

  protected Auction() {
    // No-arg constructor para que Spring Data Mongo pueda hidratar via field reflection.
  }

  public Auction(User user, Card card, Integer auctionDurationHours, List<AuctionCondition> conditions) {
    this.publisherUser = user;
    this.publisherName = user.getName();
    this.publisherAvatarId = user.getAvatarId();
    this.card = card;
    this.cardId = card.getId();
    this.cardNumber = card.getNumber();
    this.cardDescription = card.getDescription();
    this.cardCountry = card.getCountry();
    this.cardTeam = card.getTeam();
    this.cardCategory = card.getCategory();
    this.cardType = card.getType();
    this.creationDate = LocalDateTime.now();
    this.closeDate = this.creationDate.plusHours(auctionDurationHours);
    this.conditions = new ArrayList<>(conditions == null ? List.of() : conditions);
  }

  public void addOffer(AuctionOffer auctionOffer) {
    checkConditions(auctionOffer);
    this.offers.add(auctionOffer);
  }

  public boolean checkConditions(AuctionOffer auctionOffer) {
    for (AuctionCondition condition : conditions) {
      if (!condition.canOffer(auctionOffer.getBidder(), auctionOffer)) {
        throw new UnprocessableException("No cumplís las condiciones mínimas para ofertar en esta subasta");
      }
    }
    return true;
  }

  public void rejectOffer(AuctionOffer offer) throws NotFoundException, ConflictException {
    if (!offers.contains(offer)) {
      throw new NotFoundException("La oferta no pertenece a esta subasta");
    }
    if (!offer.isPending()) {
      throw new ConflictException("La oferta ya fue rechazada previamente");
    }
    offer.reject();
  }

  public boolean allowsOfferAcceptance() {
    return this.status == AuctionStatus.ACTIVE;
  }

  public void acceptOffer(AuctionOffer offer) throws ConflictException, ConflictException, NotFoundException {
    if (!allowsOfferAcceptance()) {
      throw new ConflictException("La subasta ya está cerrada");
    }
    if (!offer.isPending()) {
      throw new ConflictException("La oferta ya fue aceptada o rechazada previamente");
    }
    findOfferById(offer.getId());
    offer.accept();
    this.status = AuctionStatus.AWARDED;
    this.setBestOffer(offer);

    if (this.offers == null) {
      return;
    }

    this.offers.stream()
        .filter(AuctionOffer::isPending)
        .forEach(AuctionOffer::reject);
  }

  public void addInterestedUser(User user) {
    boolean alreadyInterested = this.interestedUsers.stream()
        .anyMatch(u -> Objects.equals(u.getId(), user.getId()));
    if (!alreadyInterested) {
      this.interestedUsers.add(user);
    }
  }

  public boolean notCancelled() {
    return this.status != AuctionStatus.CANCELLED;
  }

  /**
   * Cancela la subasta como invariante atómico:
   *   - Cambia status a CANCELLED.
   *   - Rechaza todas las offers que estaban PENDING (las que ya estaban REJECTED no se tocan).
   *
   * <p>Devuelve un {@link CancelResult} con (a) los commits a liberar — publisher + bidders cuyas
   * offers se rechazan en este paso — y (b) los bidder IDs a los que hay que notificar la
   * cancelación. El service aplica esos cambios contra el {@code UserRepository} y dispara las
   * notificaciones.
   */
  public CancelResult cancel() {
    if (this.status != AuctionStatus.ACTIVE) {
      throw new ConflictException("Sólo se puede cancelar una subasta activa");
    }
    if (isExpired()) {
      throw new ConflictException("La subasta ya expiró");
    }
    return closeRejectingAllOffers();
  }

  /**
   * Cierra una subasta vencida que termina sin ganador (camino del cron de cierre): rechaza las
   * offers pendientes y libera los commits, igual que {@link #cancel()} pero <strong>sin</strong> el
   * guard de expiración — este camino es justamente para subastas ya vencidas.
   */
  public CancelResult closeWithoutWinner() {
    if (this.status != AuctionStatus.ACTIVE) {
      throw new ConflictException("La subasta ya está cerrada");
    }
    return closeRejectingAllOffers();
  }

  private CancelResult closeRejectingAllOffers() {
    this.status = AuctionStatus.CANCELLED;

    List<CommitRelease> releases = new ArrayList<>();
    List<String> notifyBidderIds = new ArrayList<>();

    // Publisher recupera 1 unidad de la card subastada (la que comprometió al crear la subasta).
    releases.add(new CommitRelease(this.publisherUser.getId(), this.card.getId(), 1));

    for (AuctionOffer offer : this.offers) {
      if (!offer.isPending()) continue;
      offer.reject();
      notifyBidderIds.add(offer.getBidderId());
      for (AuctionItem oi : offer.getOfferedItems()) {
        releases.add(new CommitRelease(offer.getBidderId(), oi.getCard().getId(), oi.getAmount()));
      }
    }

    return new CancelResult(releases, notifyBidderIds);
  }

  /**
   * Selecciona automáticamente la oferta ganadora al cerrar por vencimiento, para cuando el
   * publisher no eligió una manualmente (no seteó {@code bestOffer}). Devuelve {@code null} si no
   * hay ofertas pendientes (la subasta se cierra sin ganador).
   *
   * <p>Criterio: las dimensiones cuya condición está presente en la subasta se priorizan (eso es
   * "lo que le interesa al subastante"); entre ellas y con las demás se respeta el orden fijo
   * <strong>rareza &gt; cantidad &gt; rating</strong>, y la oferta más temprana desempata. Ver
   * {@link OfferRankingMetric}.
   */
  public AuctionOffer selectBestOffer() {
    return this.offers.stream()
        .filter(AuctionOffer::isPending)
        .min(bestOfferComparator())
        .orElse(null);
  }

  /** Comparator "mejor primero" (para {@code min}): dimensiones priorizadas, luego desempate por fecha. */
  private Comparator<AuctionOffer> bestOfferComparator() {
    Comparator<AuctionOffer> comparator = null;
    for (OfferRankingMetric metric : rankingOrder()) {
      Comparator<AuctionOffer> byMetric = descendingBy(metric);
      comparator = (comparator == null) ? byMetric : comparator.thenComparing(byMetric);
    }
    // Desempate determinístico: la oferta más temprana gana.
    return comparator.thenComparing(AuctionOffer::getBidDate);
  }

  /**
   * Orden de dimensiones para esta subasta: primero las promovidas por sus condiciones (en el orden
   * fijo de {@link OfferRankingMetric}), después el resto en ese mismo orden.
   */
  private List<OfferRankingMetric> rankingOrder() {
    Set<OfferRankingMetric> promoted = this.conditions.stream()
        .map(AuctionCondition::rankingMetric)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    List<OfferRankingMetric> order = new ArrayList<>();
    for (OfferRankingMetric metric : OfferRankingMetric.values()) {
      if (promoted.contains(metric)) order.add(metric);
    }
    for (OfferRankingMetric metric : OfferRankingMetric.values()) {
      if (!promoted.contains(metric)) order.add(metric);
    }
    return order;
  }

  private static Comparator<AuctionOffer> descendingBy(OfferRankingMetric metric) {
    return switch (metric) {
      case RARITY -> Comparator.comparingInt(Auction::maxRarity).reversed();
      case QUANTITY -> Comparator.comparingInt(Auction::totalQuantity).reversed();
      case RATING -> Comparator.comparingDouble(Auction::bidderRating).reversed();
    };
  }

  /** Rareza = la categoría más alta entre las cartas ofrecidas (ordinal: COMMON 0 &lt; EPIC 1 &lt; LEGENDARY 2). */
  private static int maxRarity(AuctionOffer offer) {
    return offer.getOfferedItems().stream()
        .filter(item -> item.getCard() != null && item.getCard().getCategory() != null)
        .mapToInt(item -> item.getCard().getCategory().ordinal())
        .max()
        .orElse(-1);
  }

  private static int totalQuantity(AuctionOffer offer) {
    return offer.getOfferedItems().stream()
        .mapToInt(item -> item.getAmount() == null ? 0 : item.getAmount())
        .sum();
  }

  private static double bidderRating(AuctionOffer offer) {
    return offer.getBidderRating() != null ? offer.getBidderRating() : 0.0;
  }

  public boolean isExpired() {
    return this.closeDate != null && this.closeDate.isBefore(LocalDateTime.now());
  }

  public void validateOwner(User user) {
    if (!this.publisherUser.getId().equals(user.getId())) {
      throw new ForbiddenException("Operación no permitida. No es dueño de la subasta");
    }
  }

  public AuctionOffer findOfferById(String offerId) {
    return this.offers.stream()
        .filter(offer -> offer.getId().equals(offerId))
        .findFirst().orElseThrow(() -> new UnprocessableException("La oferta no existe en esta subasta"));
  }

  public void changeBestOffer(AuctionOffer offer) {
    if (offer.getStatus().equals(AuctionOfferStatus.REJECTED)) {
      throw new UnprocessableException("No se puede elegir como mejor oferta una que ya fue rechazada");
    }
    this.setBestOffer(offer);
  }
}
