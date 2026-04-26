package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.services.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Handles everything related to cards as catalog entries (not as user subdocuments)
 * /cards/catalog         → full catalog (all existing cards, read-only)
 * /cards/catalog/{id}    → detail of a single catalog card
 * /cards/search          → search cards available in active listings and auctions (pending)
 */
@RestController
@RequestMapping("/cards")
public class CardsController {

    private final CardService cardService;

    public CardsController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * Returns the full card catalog (aún no hay api disponible para bajarnos las figuritas oficiales, el 27/4 se estrenan creo)
     * @return list of all cards
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<Card>> getCatalog() {
        return ResponseEntity.ok(cardService.getCatalog());
    }

    /**
     * Returns a single card from the catalog by its ID
     * @param id the card's MongoDB ID
     * @return the card, or 404 if not found
     */
    @GetMapping("/catalog/{id}")
    public ResponseEntity<Card> getCatalogById(@PathVariable String id) {
        return ResponseEntity.ok(cardService.getById(id));
    }

    /**
     * Searches for cards available in active listings and auctions
     * Returns 501 until PublicacionesService and SubastasService are migrated to MongoDB
     */
    @GetMapping("/search")
    public ResponseEntity<Void> searchAvailable(
            @RequestParam(required = false) Integer number,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        return ResponseEntity.status(501).build();
    }
}
