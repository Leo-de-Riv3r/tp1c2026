package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.entities.profiles.Profile;
import com.tacs.tp1c2026.entities.profiles.ProfileGroup;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.Suggestion;
import com.tacs.tp1c2026.properties.ProfileProperties;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.ProfileGroupRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProfileService {

    private final ProfileGroupRepository profileGroupRepository;
    private final UserService userService;
    private final PublicationRepository publicationRepository;
    private final AuctionRepository auctionRepository;
    private final ProfileProperties properties;

    public ProfileService(ProfileGroupRepository profileGroupRepository,
                          UserService userService,
                          PublicationRepository publicationRepository,
                          AuctionRepository auctionRepository,
                          ProfileProperties properties) {
        this.profileGroupRepository = profileGroupRepository;
        this.userService = userService;
        this.publicationRepository = publicationRepository;
        this.auctionRepository = auctionRepository;
        this.properties = properties;
    }

    /**
     * Cron que regenera las sugerencias de TODOS los users.
     * <p>
     * Algoritmo por user:
     * <ol>
     *   <li>Pool de candidatos: vecinos de los top {@code profileGroupsToCheck} grupos
     *       ordenados por similitud al perfil del user, excluyendo al user mismo y deduplicados.</li>
     *   <li>Batch + retry: se evalúan {@code candidatesPerBatch} candidatos a la vez. Si el batch
     *       no produce sugerencias, se intenta con el siguiente batch (hasta {@code maxBatches}).</li>
     *   <li>Para cada candidato del batch, se buscan sus publications/auctions ACTIVE que
     *       contengan alguna de las missing cards del user. Cada match se vuelve una Suggestion.</li>
     *   <li>Cap final de {@code maxSuggestionsPerUser} (default 10) por user.</li>
     * </ol>
     * Las publications/auctions ACTIVE se cargan UNA sola vez por corrida y se indexan por publisher
     * para que el matching sea O(1) por candidato (en lugar de N llamadas al repo).
     */
    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class },
               maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void updateSuggestionsForUsers() {
        List<ProfileGroup> groups = this.profileGroupRepository.findAll();
        List<User> users = userService.getAll();

        // Index de publications/auctions ACTIVE por publisher.id — una sola carga por corrida
        Map<String, List<TradePublication>> activePubsByPublisher = publicationRepository
            .findByStatus(PublicationStatus.ACTIVE).stream()
            .filter(p -> p.getPublisherUser() != null)
            .collect(Collectors.groupingBy(p -> p.getPublisherUser().getId()));

        Map<String, List<Auction>> activeAuctionsByPublisher = auctionRepository
            .findByStatus(AuctionStatus.ACTIVE).stream()
            .filter(a -> a.getPublisherUser() != null)
            .collect(Collectors.groupingBy(a -> a.getPublisherUser().getId()));

        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            List<Suggestion> suggestions = generateSuggestionsFor(
                user, groups, activePubsByPublisher, activeAuctionsByPublisher, now);
            user.updateSuggestions(suggestions);
        }

        this.userService.saveAll(users);

        for (ProfileGroup pfg : groups) {
            pfg.updateVector();
        }
        this.profileGroupRepository.saveAll(groups);
    }

    private List<Suggestion> generateSuggestionsFor(
            User user,
            List<ProfileGroup> groups,
            Map<String, List<TradePublication>> pubsByPublisher,
            Map<String, List<Auction>> aucsByPublisher,
            LocalDateTime now) {

        Set<String> missingCardIds = user.getMissingCards().stream()
            .map(mc -> mc.getCardId())
            .collect(Collectors.toSet());
        if (missingCardIds.isEmpty()) return List.of();

        // Pool: vecinos de los top N grupos, ordenados por similitud, sin el user mismo, deduplicados
        List<User> pool = groups.stream()
            .sorted(Comparator.comparingInt(g -> Profile.complement(g.getRepresentativeProfile(), user.getProfile())))
            .limit(properties.getProfileGroupsToCheck())
            .flatMap(g -> g.getNeighbours().stream())
            .filter(other -> !other.getId().equals(user.getId()))
            .distinct()
            .toList();

        // Batch + retry: si un batch no produce sugerencias, intenta con el siguiente
        int batchSize = properties.getCandidatesPerBatch();
        int maxBatches = properties.getMaxBatches();
        int maxSuggestions = properties.getMaxSuggestionsPerUser();

        List<Suggestion> suggestions = List.of();
        for (int batch = 0; batch < maxBatches && suggestions.isEmpty(); batch++) {
            List<User> candidates = pool.stream()
                .skip((long) batch * batchSize)
                .limit(batchSize)
                .toList();
            if (candidates.isEmpty()) break;

            suggestions = candidates.stream()
                .flatMap(candidate -> matchingSourcesFor(candidate, missingCardIds, pubsByPublisher, aucsByPublisher, now))
                .limit(maxSuggestions)
                .collect(Collectors.toCollection(ArrayList::new));
        }

        return suggestions;
    }

    /**
     * Devuelve un stream de Suggestion para una publication/auction activa del candidato
     * cuya card está en las missing cards del current user.
     */
    private Stream<Suggestion> matchingSourcesFor(
            User candidate,
            Set<String> missingCardIds,
            Map<String, List<TradePublication>> pubsByPublisher,
            Map<String, List<Auction>> aucsByPublisher,
            LocalDateTime now) {

        Stream<Suggestion> pubMatches = pubsByPublisher
            .getOrDefault(candidate.getId(), List.of()).stream()
            .filter(pub -> pub.getCard() != null && missingCardIds.contains(pub.getCard().getId()))
            .map(pub -> Suggestion.fromPublication(pub, now));

        Stream<Suggestion> aucMatches = aucsByPublisher
            .getOrDefault(candidate.getId(), List.of()).stream()
            .filter(auc -> auc.getCard() != null && missingCardIds.contains(auc.getCard().getId()))
            .map(auc -> Suggestion.fromAuction(auc, now));

        return Stream.concat(pubMatches, aucMatches);
    }

    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void updateProfileGroups(User user){
        List<ProfileGroup> pfg = this.profileGroupRepository.findAll();

        for (ProfileGroup p : pfg) {
            p.removeNeighbor(user);
        }

        List<ProfileGroup> newGroups = pfg.stream()
                .sorted(Comparator.comparingInt(g -> Profile.agreement(g.getRepresentativeProfile(), user.getProfile())))
                .limit(this.properties.getMaximumNumberOfGroupsUserCanBeIn())
                .toList();

        newGroups.forEach(g -> g.addNeighbor(user));

        this.profileGroupRepository.saveAll(pfg);
    }


}
