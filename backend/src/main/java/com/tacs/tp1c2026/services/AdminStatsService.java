package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.config.CacheConfig;
import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.dto.statistics.output.MostWantedCardEntry;
import com.tacs.tp1c2026.entities.dto.statistics.output.OverviewDto;
import com.tacs.tp1c2026.entities.dto.statistics.output.TopAuctionByOffersEntry;
import com.tacs.tp1c2026.entities.dto.statistics.output.TopExchangedCardEntry;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.exchange.Exchange;
import com.tacs.tp1c2026.entities.exchange.embedded.CardSnapshot;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.ExchangeRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Métricas para el dashboard del admin. Sirve las 3 capas del diseño:
 * <ul>
 *   <li><b>Capa 1</b> (counts puntuales): {@link #getOverview()} usa {@code count()} con índice, sin cache.</li>
 *   <li><b>Capa 3</b> (top-N): los 3 highlights ({@link #getMostWantedCardsInLastDays(int)},
 *       {@link #getTopExchangedCardsInLastDays(int)}, {@link #getTopAuctionByOffers()}) corren live
 *       y van cacheados con TTL 5 min (ver {@link CacheConfig}).</li>
 * </ul>
 * Capa 2 (timeseries por snapshot) vive en {@code StatsSnapshotService}.
 */
@Service
public class AdminStatsService {

    private static final int TOP_N = 5;

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final PublicationRepository publicationRepository;
    private final ExchangeRepository exchangeRepository;

    public AdminStatsService(UserRepository userRepository,
                             AuctionRepository auctionRepository,
                             PublicationRepository publicationRepository,
                             ExchangeRepository exchangeRepository) {
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.publicationRepository = publicationRepository;
        this.exchangeRepository = exchangeRepository;
    }

    /**
     * Snapshot del estado actual del sistema. Cuatro counts triviales (índice + O(log n)), live.
     * No incluye filtro de role para {@code totalUsers}: el dashboard cuenta todos los registros.
     */
    public OverviewDto getOverview() {
        return new OverviewDto(
            userRepository.count(),
            auctionRepository.countByStatus(AuctionStatus.ACTIVE),
            publicationRepository.countByStatus(PublicationStatus.ACTIVE),
            exchangeRepository.count()
        );
    }

    /**
     * Cartas más buscadas (en {@code missingCards}) en los últimos {@code days} días,
     * ordenadas descendente por cantidad de users que la buscan.
     */
    @Cacheable(value = CacheConfig.ADMIN_STATS_CACHE, key = "'most-wanted:' + #days")
    public List<MostWantedCardEntry> getMostWantedCardsInLastDays(int days) {
        if (days <= 0) return List.of();
        LocalDate cutoff = LocalDate.now().minusDays(days);

        List<MissingCard> recent = userRepository.findAll().stream()
            .flatMap(user -> user.getMissingCards().stream())
            .filter(mc -> mc.getAddedAt() != null && !mc.getAddedAt().isBefore(cutoff))
            .toList();

        Map<String, Long> countByCardId = recent.stream()
            .collect(Collectors.groupingBy(MissingCard::getCardId, Collectors.counting()));

        Map<String, MissingCard> sampleByCardId = recent.stream()
            .collect(Collectors.toMap(MissingCard::getCardId, mc -> mc, (a, b) -> a));

        return countByCardId.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(TOP_N)
            .map(e -> {
                MissingCard sample = sampleByCardId.get(e.getKey());
                return new MostWantedCardEntry(
                    e.getKey(),
                    sample.getNumber(),
                    sample.getDescription(),
                    e.getValue(),
                    days
                );
            })
            .toList();
    }

    /**
     * Top cartas más intercambiadas en los últimos {@code days} días. Cada aparición en
     * {@code cardsFromA} o {@code cardsFromB} de un Exchange suma 1 — refleja "veces que la card
     * cambió de manos", incluyendo las dos puntas del intercambio.
     */
    @Cacheable(value = CacheConfig.ADMIN_STATS_CACHE, key = "'top-exchanged:' + #days")
    public List<TopExchangedCardEntry> getTopExchangedCardsInLastDays(int days) {
        if (days <= 0) return List.of();
        LocalDateTime cutoff = LocalDate.now().minusDays(days).atStartOfDay();

        List<CardSnapshot> all = exchangeRepository.findByCreatedAtAfter(cutoff).stream()
            .flatMap(ex -> Stream.concat(
                safeStream(ex.getCardsFromA()),
                safeStream(ex.getCardsFromB())))
            .toList();

        Map<String, Long> countByCardId = all.stream()
            .collect(Collectors.groupingBy(CardSnapshot::getCardId, Collectors.counting()));

        Map<String, CardSnapshot> sampleByCardId = all.stream()
            .collect(Collectors.toMap(CardSnapshot::getCardId, c -> c, (a, b) -> a));

        return countByCardId.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(TOP_N)
            .map(e -> {
                CardSnapshot sample = sampleByCardId.get(e.getKey());
                return new TopExchangedCardEntry(
                    e.getKey(),
                    sample.getNumber(),
                    sample.getDescription(),
                    e.getValue(),
                    days
                );
            })
            .toList();
    }

    /**
     * Subasta activa con más ofertas PENDING. {@code totalOffers} (incluye REJECTED/CANCELLED) se
     * agrega como contexto, pero el orden lo decide {@code pendingOffers}. {@code Optional.empty()}
     * si no hay subastas activas con ofertas.
     */
    @Cacheable(value = CacheConfig.ADMIN_STATS_CACHE, key = "'top-auction-offers'")
    public Optional<TopAuctionByOffersEntry> getTopAuctionByOffers() {
        return auctionRepository.findByStatus(AuctionStatus.ACTIVE).stream()
            .map(a -> new TopAuctionByOffersEntry(
                a.getId(),
                a.getCardId(),
                a.getCardDescription(),
                a.getPublisherName(),
                a.getOffers() == null ? 0 : a.getOffers().stream().filter(AuctionOffer::isPending).count(),
                a.getOffers() == null ? 0 : a.getOffers().size()))
            .filter(e -> e.pendingOffers() > 0)
            .max(Comparator.comparingLong(TopAuctionByOffersEntry::pendingOffers));
    }

    private static <T> Stream<T> safeStream(List<T> list) {
        return list == null ? Stream.empty() : list.stream();
    }
}
