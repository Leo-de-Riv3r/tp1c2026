package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.User;
import com.tacs.tp1c2026.entities.StickerCollection;
import com.tacs.tp1c2026.entities.FiguritaFaltante;
import com.tacs.tp1c2026.entities.dto.input.usuario.*;
import com.tacs.tp1c2026.services.UsuariosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UsuariosController {

    private final UsuariosService usuariosService;

    public UsuariosController(UsuariosService usuariosService) {
        this.usuariosService = usuariosService;
    }

    // Todos los users - para la view del admin - ver si se usa o despues descartar
    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(usuariosService.listarUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(usuariosService.obtenerUsuario(id));
    }

    /* Métodos para operar sobre la colección del usuario */

    @GetMapping("/{id}/collection")
    public ResponseEntity<List<StickerCollection>> getCollection(@PathVariable Integer id) {
        return ResponseEntity.ok(usuariosService.obtenerColeccion(id));
    }

    // Agrega una figurita a la colección o incrementa la cantidad si ya existía
    @PostMapping("/{id}/collection")
    public ResponseEntity<StickerCollection> addToCollection(
            @PathVariable Integer id,
            @Valid @RequestBody AddToCollectionRequest request) {
        return ResponseEntity.ok(usuariosService.agregarAColeccion(id, request.figuritaId()));
    }

    // Decrementa la cantidad de una figurita de la colección, si llega a cero la quita
    @PatchMapping("/{id}/collection/{figuritaId}")
    public ResponseEntity<Void> decrementFromCollection(
            @PathVariable Integer id,
            @PathVariable Integer figuritaId) {
        usuariosService.decrementFromCollection(id, figuritaId);
        return ResponseEntity.noContent().build();
    }

    /* Métodos que operan sobre la lista de faltantes del usuario */

    @GetMapping("/{id}/missing-cards")
    public ResponseEntity<List<FiguritaFaltante>> getMissingCards(@PathVariable Integer id) {
        return ResponseEntity.ok(usuariosService.obtenerFaltantes(id));
    }

    // Marca una figurita como faltante
    @PostMapping("/{id}/missing-cards")
    public ResponseEntity<FiguritaFaltante> addMissingCard(
            @PathVariable Integer id,
            @Valid @RequestBody AddMissingCardRequest request) {
        return ResponseEntity.ok(usuariosService.agregarFaltante(id, request.figuritaId()));
    }

    /* Métodos para las sugerencias - No sé qué más se hará sobre esto, asi que dejo este x ahora */

    // @GetMapping("/{id}/suggestions")
    // public ResponseEntity<List<Sugerencia>> getSuggestions(@PathVariable String id) {
    //     return ResponseEntity.ok(usuariosService.obtenerSugerencias(id));
    // }
}
