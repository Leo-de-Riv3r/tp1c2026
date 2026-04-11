package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.dto.PublicacionCreationDTO;
import com.tacs.tp1c2026.entities.Publicacion;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.services.PublicacionService;
import com.tacs.tp1c2026.services.UsuarioService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/publicaciones")
public class PublicacionController {

    private PublicacionService publicacionService;
    private UsuarioService usuarioService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @PostMapping("/")
    public void addCard(@PathVariable Long userId, PublicacionCreationDTO publicacionCreationDTO) {
        Usuario user = this.usuarioService.getById(userId);
        Publicacion figurita = this.publicacionService.createPublicacion(publicacionCreationDTO);
        this.usuarioService.addFiguritaColeccionToUser(user, figurita);
    }

}
