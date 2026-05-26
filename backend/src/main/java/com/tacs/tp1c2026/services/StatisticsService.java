package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.dto.statistics.output.MostWantedCardEntry;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final UserRepository userRepository;

    public StatisticsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Cartas más buscadas (en {@code missingCards}) en los últimos {@code days} días,
     * ordenadas descendente por cantidad de users que la buscan.
     * <p>
     * Lógica: itera todos los users, filtra sus missing cards por {@code addedAt >= hoy - days},
     * agrupa por cardId y cuenta. Devuelve la lista ordenada con número y descripción
     * (snapshot del primer MissingCard que aparece para ese cardId) — el FE no necesita
     * un fetch adicional al catálogo
     */
    public List<MostWantedCardEntry> getMostWantedCardsInLastDays(int days) {
        if (days <= 0) return List.of();
        LocalDate cutoff = LocalDate.now().minusDays(days);

        List<MissingCard> recent = userRepository.findAll().stream()
            .flatMap(user -> user.getMissingCards().stream())
            .filter(mc -> mc.getAddedAt() != null && !mc.getAddedAt().isBefore(cutoff))
            .toList();

        // count por cardId
        Map<String, Long> countByCardId = recent.stream()
            .collect(Collectors.groupingBy(MissingCard::getCardId, Collectors.counting()));

        // representante por cardId (para tomar number/description del snapshot)
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
