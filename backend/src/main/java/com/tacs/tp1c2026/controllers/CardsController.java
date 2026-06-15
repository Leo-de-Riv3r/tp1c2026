package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.card.output.SearchCardsResponseDto;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.enums.CardType;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.services.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Maneja todo lo relacionado a figuritas como entradas del catálogo (no como subdocumentos del user).
 * /cards/catalog         → catálogo completo (todas las figuritas existentes, read-only)
 * /cards/catalog/{id}    → detalle de una figurita del catálogo
 * /cards/search          → busca figuritas disponibles en publicaciones y subastas activas
 */
@RestController
@RequestMapping("/cards")
public class CardsController {

    private final CardService cardService;

    public CardsController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * Devuelve el catálogo completo de figuritas (aún no hay API disponible para bajarnos las figuritas oficiales, el 27/4 se estrenan creo).
     * @return lista de todas las figuritas
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<Card>> getCatalog() {
        return ResponseEntity.ok(cardService.getCatalog());
    }

    /**
     * Devuelve una sola figurita del catálogo por su ID.
     * @param id el ID en Mongo de la figurita
     * @return la figurita, o 404 si no se encuentra
     */
    @GetMapping("/catalog/{id}")
    public ResponseEntity<Card> getCatalogById(@PathVariable String id) throws NotFoundException {
        return ResponseEntity.ok(cardService.getById(id));
    }

    /**
     * Busca figuritas disponibles en publicaciones y subastas activas con filtros.
     * Cada lista se pagina independiente con {@code pubPage}/{@code pubPerPage} y
     * {@code aucPage}/{@code aucPerPage} (default page=1, perPage=10).
     */
    @GetMapping("/search")
    public ResponseEntity<SearchCardsResponseDto> searchAvailable(
            @RequestAttribute("userId") String currentUserId,
            @RequestParam(required = false) Integer number,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) CardType cardType,
            @RequestParam(defaultValue = "1") Integer pubPage,
            @RequestParam(defaultValue = "10") Integer pubPerPage,
            @RequestParam(defaultValue = "1") Integer aucPage,
            @RequestParam(defaultValue = "10") Integer aucPerPage) {
        // Excluye las publicaciones/subastas del propio user (no tiene sentido intercambiar con uno mismo).
        SearchPublicationsFilters filters = new SearchPublicationsFilters(
            description, country, team, category, cardType, number, currentUserId);
        return ResponseEntity.ok(
            cardService.searchInActiveListings(filters, pubPage, pubPerPage, aucPage, aucPerPage));
    }
}
