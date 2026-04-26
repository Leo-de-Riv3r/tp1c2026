package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.CardRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * Returns the full card catalog
     * @return list of all {@link Card} documents
     */
    public List<Card> getCatalog() {
        return cardRepository.findAll();
    }

    /**
     * Returns a single card from the catalog by its ID
     * Used to fill selection combos in the frontend (add to collection, add missing card, etc.)
     * @param id the card's MongoDB ID
     * @return the {@link Card}, or throws if not found
     * @throws NotFoundException if no card exists with that ID
     */
    public Card getById(String id) {
        return cardRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Card not found: " + id));
    }

    /**
     * Searches for cards available in active listings and auctions, with optional filters
     * Results are grouped by cardId — one entry per card with the active listings/auctions that have it
     * TODO: implement when PublicacionesService and SubastasService are migrated to MongoDB.
     *
     * @param number      exact card number
     * @param description partial text match on description
     * @param country     team country (e.g. "Argentina")
     * @param category    card category (e.g. "LEGENDARIO")
     * @param type        card type (e.g. "JUGADOR")
     */
    public Void searchAvailable(Integer number, String description,
                                String country, String category, String type) {
        throw new UnsupportedOperationException("searchAvailable: pendiente de implementación");
    }
}
