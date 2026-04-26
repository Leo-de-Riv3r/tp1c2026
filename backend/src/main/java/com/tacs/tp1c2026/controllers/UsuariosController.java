package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.User;
import com.tacs.tp1c2026.entities.StickerCollection;
import com.tacs.tp1c2026.entities.FiguritaFaltante;
import com.tacs.tp1c2026.entities.dto.input.usuario.*;
import com.tacs.tp1c2026.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UsuariosController {

    private final UserService userService;

    public UsuariosController(UserService userService) {
        this.userService = userService;
    }

    // Todos los users - para la view del admin - ver si se usa o despues descartar
    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.listarUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.obtenerUsuario(id));
    }

    /* Métodos para operar sobre la colección del usuario */

    @GetMapping("/{id}/collection")
    public ResponseEntity<List<StickerCollection>> getCollection(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.obtenerColeccion(id));
    }

    // Agrega una figurita a la colección o incrementa la cantidad si ya existía
    @PostMapping("/{id}/collection")
    public ResponseEntity<StickerCollection> addToCollection(
            @PathVariable Integer id,
            @Valid @RequestBody AddToCollectionRequest request) {
        return ResponseEntity.ok(userService.agregarAColeccion(id, request.figuritaId()));
    }

    // Decrementa la cantidad de una figurita de la colección, si llega a cero la quita
    @PatchMapping("/{id}/collection/{figuritaId}")
    public ResponseEntity<Void> decrementFromCollection(
            @PathVariable Integer id,
            @PathVariable Integer figuritaId) {
        userService.decrementFromCollection(id, figuritaId);
        return ResponseEntity.noContent().build();
    }

    /* Métodos que operan sobre la lista de faltantes del usuario */

    @GetMapping("/{id}/missing-cards")
    public ResponseEntity<List<FiguritaFaltante>> getMissingCards(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.obtenerFaltantes(id));
    }

    // Marca una figurita como faltante
    @PostMapping("/{id}/missing-cards")
    public ResponseEntity<FiguritaFaltante> addMissingCard(
            @PathVariable Integer id,
            @Valid @RequestBody AddMissingCardRequest request) {
        return ResponseEntity.ok(userService.agregarFaltante(id, request.figuritaId()));
    }

    /* Métodos para las sugerencias - No sé qué más se hará sobre esto, asi que dejo este x ahora */

    // @GetMapping("/{id}/suggestions")
    // public ResponseEntity<List<Sugerencia>> getSuggestions(@PathVariable String id) {
    //     return ResponseEntity.ok(usuariosService.obtenerSugerencias(id));
    // }
}
