package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.Card;
import com.tacs.tp1c2026.services.CardsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/*
    Controller para todo lo relacionado a las figuritas en sí, sin ser subdocumento del usuario
    /figuritas/catalog -> catálogo (todas las figuritas que existen, fijas)
    /figuritas/catalog/{id} -> para el detalle de una card del catalogo
    /figuritas/search -> búsqueda de figuritas disponbiles en publicaciones y subastas activas (a implementar cuando estén migradas esas colecciones)
 */
@RestController
@RequestMapping("/catalog")
public class FiguritasController {

    private final CardsService cardsService;

    public FiguritasController(CardsService cardsService) {
        this.cardsService = cardsService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Card>> getCatalog() {
        return ResponseEntity.ok(cardsService.getCatalog());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Card> getCatalogById(@PathVariable String id) {
        return ResponseEntity.ok(cardsService.getById(id));
    }
}
