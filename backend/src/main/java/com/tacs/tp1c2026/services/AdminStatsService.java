package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.dto.statistics.output.MostWantedCardEntry;
import com.tacs.tp1c2026.entities.dto.statistics.output.OverviewDto;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.ExchangeRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Métricas para el dashboard del admin. Sirve las 3 capas del diseño:
 * <ul>
 *   <li><b>Capa 1</b> (counts puntuales): {@link #getOverview()} usa {@code count()} con índice,
 *       sin cache.</li>
 *   <li><b>Capa 3</b> (top-N): {@link #getMostWantedCardsInLastDays(int)} corre aggregation live.</li>
 * </ul>
 * Capa 2 (timeseries por snapshot) vive en {@code StatsSnapshotService}.
 */
@Service
public class AdminStatsService {

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
     * <p>
     * Lógica: itera todos los users, filtra sus missing cards por {@code addedAt >= hoy - days},
     * agrupa por cardId y cuenta. Devuelve la lista ordenada con número y descripción
     * (snapshot del primer MissingCard que aparece para ese cardId) — el FE no necesita
     * un fetch adicional al catálogo.
     */
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
}
