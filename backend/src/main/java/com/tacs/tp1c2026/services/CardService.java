package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.card.input.CardSearchParamsDTO;
import com.tacs.tp1c2026.entities.dto.card.input.IndicateMissingCardDTO;
import com.tacs.tp1c2026.entities.dto.card.input.PublishCardDTO;
import com.tacs.tp1c2026.entities.dto.card.output.CardDTO;
import com.tacs.tp1c2026.entities.dto.card.output.SearchCardsResponseDto;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.mappers.AuctionMapper;
import com.tacs.tp1c2026.entities.dto.mappers.CardMapper;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.CardRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.services.mappers.TradeMapper;
import com.tacs.tp1c2026.utils.PageableGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final AuctionRepository auctionRepository;
    private final TradeMapper tradeMapper;
    private final AuctionMapper auctionMapper;
    private final PageableGenerator pageableGenerator;

    public CardService(CardRepository cardRepository,
                       UserRepository userRepository,
                       PublicationRepository publicationRepository,
                       AuctionRepository auctionRepository,
                       TradeMapper tradeMapper,
                       AuctionMapper auctionMapper,
                       PageableGenerator pageableGenerator) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.publicationRepository = publicationRepository;
        this.auctionRepository = auctionRepository;
        this.tradeMapper = tradeMapper;
        this.auctionMapper = auctionMapper;
        this.pageableGenerator = pageableGenerator;
    }

    /**
     * Busca figuritas disponibles en publicaciones y subastas activas que matcheen los filtros.
     * Cada lista se pagina por separado (default 10 por página) para que el FE pueda mostrar
     * paginadores independientes y mantener performance con catálogos grandes.
     */
    public SearchCardsResponseDto searchInActiveListings(SearchPublicationsFilters filters,
                                                          Integer pubPage, Integer pubPerPage,
                                                          Integer aucPage, Integer aucPerPage) {
        Pageable pubPageable = pageableGenerator.buildPageable(pubPage, pubPerPage, 10, null);
        Pageable aucPageable = pageableGenerator.buildPageable(aucPage, aucPerPage, 10, null);

        Page<com.tacs.tp1c2026.entities.exchange.TradePublication> pubResult =
            publicationRepository.searchWithFilters(filters, pubPageable);
        Page<com.tacs.tp1c2026.entities.auction.Auction> aucResult =
            auctionRepository.searchWithFilters(filters, aucPageable);

        return new SearchCardsResponseDto(
            new PaginationDtoOutput<>(
                tradeMapper.mapPublications(pubResult.getContent()),
                pubResult.getNumber() + 1,
                pubResult.getTotalPages()),
            new PaginationDtoOutput<>(
                auctionMapper.mapAuctions(aucResult.getContent()),
                aucResult.getNumber() + 1,
                aucResult.getTotalPages())
        );
    }

    /**
     * Returns the full card catalog
     * @return list of all {@link Card} documents
     */
    public List<Card> getCatalog() {
        return cardRepository.findAll();
    }

    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void publishCard(String userId, PublishCardDTO dto){
        User user = this.userRepository.findOrThrow(userId);
        Card card = this.cardRepository.findOrThrow(dto.cardId());
        user.addToCollection(com.tacs.tp1c2026.entities.user.embedded.CollectionCard.fromCatalog(card));
        this.userRepository.save(user);
    }

    @Retryable(retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class }, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public void indicateMissingCard(String userId, IndicateMissingCardDTO dto){
        User user = this.userRepository.findOrThrow(userId);
        Card card = this.cardRepository.findOrThrow(dto.cardId());
        user.addToMissingCards(com.tacs.tp1c2026.entities.user.embedded.MissingCard.fromCatalog(card));
        this.userRepository.save(user);
    }

    @Transactional
    public CardDTO searchForCard(CardSearchParamsDTO dto) throws NotFoundException {
        if (dto.number() != null) {
            Card card = this.cardRepository.findByNumber(dto.number()).orElseThrow(() -> new NotFoundException("Card not found"));
            return CardMapper.toDto(card);
        }
        return this.cardRepository.findAll().stream()
                .filter(s -> matchesSearchParams(s, dto))
                .findFirst()
                .map(CardMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Card not found with given parameters"));
    }

    @Transactional
    public Card getById(String cardId) throws NotFoundException {
        return this.cardRepository.findById(cardId).orElseThrow(() -> new NotFoundException("Card not found"));
    }

    private boolean matchesSearchParams(Card s, CardSearchParamsDTO dto) {
        if (dto.description() != null) {
            String needle = dto.description().toLowerCase();
            String player = s.getPlayer() == null ? "" : s.getPlayer().toLowerCase();
            String desc = s.getDescription() == null ? "" : s.getDescription().toLowerCase();
            if (!player.contains(needle) && !desc.contains(needle)) return false;
        }
        if (dto.country() != null && (s.getCountry() == null || !s.getCountry().equalsIgnoreCase(dto.country()))) return false;
        if (dto.team() != null && (s.getTeam() == null || !s.getTeam().equalsIgnoreCase(dto.team()))) return false;
        if (dto.category() != null) {
            if (s.getCategory() == null) return false;
            return s.getCategory().toString().equalsIgnoreCase(dto.category().toString());
        }
        return true;
    }

}
