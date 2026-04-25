package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.services.FiguritasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/*
    Controller para todo lo relacionado a las figuritas en sí, sin ser subdocumento del usuario
    /figuritas/catalog -> catálogo (todas las figuritas que existen, fijas)
    /figuritas/catalog/{id} -> para el detalle de una figurita del catalogo
    /figuritas/search -> búsqueda de figuritas disponbiles en publicaciones y subastas activas (a implementar cuando estén migradas esas colecciones)
 */
@RestController
@RequestMapping("/catalog")
public class FiguritasController {

    private final FiguritasService figuritasService;

    public FiguritasController(FiguritasService figuritasService) {
        this.figuritasService = figuritasService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Figurita>> getCatalog() {
        return ResponseEntity.ok(figuritasService.getCatalog());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Figurita> getCatalogById(@PathVariable String id) {
        return ResponseEntity.ok(figuritasService.getById(id));
    }

    /*
        Búsqueda de figuritas disponibles en publicaciones y subastas activas
        TODO: implementar cuando PublicacionesService y SubastasService estén migrados a mongo
    */
    @GetMapping("/search")
    public ResponseEntity<Void> searchAvailable(
            @RequestParam(required = false) Integer number,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        return ResponseEntity.status(501).build(); // para hacer
            // figuritasService.search(number, description, country, category, type));
    }
}
