package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.card.Card;
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
    private final CardService cardService;
    private final ProfileProperties properties;

    public ProfileService(ProfileGroupRepository profileGroupRepository,
                          UserService userService,
                          PublicationRepository publicationRepository,
                          AuctionRepository auctionRepository,
                          CardService cardService,
                          ProfileProperties properties) {
        this.profileGroupRepository = profileGroupRepository;
        this.userService = userService;
        this.publicationRepository = publicationRepository;
        this.auctionRepository = auctionRepository;
        this.cardService = cardService;
        this.properties = properties;
    }

    /**
     * Siembra {@code numberOfGroups} grupos de perfil si la colección está vacía (como en cloud,
     * donde nada los inicializa). Cada grupo arranca con un perfil representativo aleatorio sobre
     * el catálogo. Sin grupos no hay pool de candidatos y las sugerencias nunca se generan.
     */
    @Transactional
    public void seedGroupsIfEmpty() {
        if (this.profileGroupRepository.count() > 0) {
            return;
        }
        List<String> catalogCardIds = this.cardService.getCatalog().stream()
            .map(Card::getId)
            .toList();
        List<ProfileGroup> groups = new ArrayList<>();
        for (int i = 0; i < this.properties.getNumberOfGroups(); i++) {
            ProfileGroup group = new ProfileGroup();
            group.initializeRepresentative(catalogCardIds);
            groups.add(group);
        }
        this.profileGroupRepository.saveAll(groups);
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
        if (groups.isEmpty()) {
            // Sin grupos de perfil no hay candidatos: recalcular dejaría a cada user con una lista
            // vacía y sobrescribiría (borraría) las sugerencias existentes. Mejor no tocar nada.
            return;
        }
        List<User> users = userService.getAll();

        // (Re)construye la pertenencia user→grupo y recalcula los representativos ANTES de generar:
        // sin esta asignación los grupos no tienen vecinos y el pool de candidatos queda siempre vacío.
        assignUsersToGroups(users, groups);
        groups.forEach(ProfileGroup::updateVector);

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
        this.profileGroupRepository.saveAll(groups);
    }

    /**
     * (Re)asigna cada user a los {@code maximumNumberOfGroupsUserCanBeIn} grupos con los que más
     * coincide (missing cards en común con el representativo), reconstruyendo desde cero la lista
     * de vecinos de cada grupo. Es la pieza que faltaba cablear: sin esto los grupos quedan sin
     * vecinos y nunca hay candidatos para las sugerencias.
     */
    private void assignUsersToGroups(List<User> users, List<ProfileGroup> groups) {
        groups.forEach(ProfileGroup::clearNeighbours);
        int maxGroups = this.properties.getMaximumNumberOfGroupsUserCanBeIn();
        for (User user : users) {
            groups.stream()
                .sorted(Comparator.comparingInt(
                    (ProfileGroup g) -> Profile.agreement(g.getRepresentativeProfile(), user.getProfile())).reversed())
                .limit(maxGroups)
                .forEach(g -> g.addNeighbor(user));
        }
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

}
