package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.card.input.CardSearchParamsDTO;
import com.tacs.tp1c2026.entities.dto.card.input.IndicateMissingCardDTO;
import com.tacs.tp1c2026.entities.dto.card.input.PublishCardDTO;
import com.tacs.tp1c2026.entities.dto.card.output.CardDTO;
import com.tacs.tp1c2026.entities.dto.mappers.CardMapper;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.CardRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public CardService(CardRepository cardRepository, UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the full card catalog
     * @return list of all {@link Card} documents
     */
    public List<Card> getCatalog() {
        return cardRepository.findAll();
    }

    @Transactional
    public void publishCard(String userId, PublishCardDTO dto){
        User user = this.userRepository.findOrThrow(userId);
        Card card = this.cardRepository.findOrThrow(dto.stickerId());
        user.addCardToCollection(card);
    }

    @Transactional
    public void indicateMissingCard(String userId, IndicateMissingCardDTO dto){
        User user = this.userRepository.findOrThrow(userId);
        Card card = this.cardRepository.findOrThrow(dto.stickerId());
        user.addMissingCard(card);
    }

    @Transactional
    public CardDTO searchForCard(CardSearchParamsDTO dto) throws NotFoundException {
        if (dto.number() != null) {
            Card card = this.cardRepository.findById(dto.number()).orElseThrow(() -> new NotFoundException("Card not found"));
            return CardMapper.toDto(card);
        }
        return this.cardRepository.findAll().stream()
                .filter(s -> matchesSearchParams(s, dto))
                .findFirst()
                .map(CardMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Card not found with given parameters"));
    }

    @Transactional
    public Card getById(Integer cardId) throws NotFoundException {
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
