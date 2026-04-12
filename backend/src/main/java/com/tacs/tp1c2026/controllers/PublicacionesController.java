package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.services.PublicacionesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionesController {
  private final PublicacionesService publicacionesService;

  public PublicacionesController(PublicacionesService publicacionesService) {
    this.publicacionesService = publicacionesService;
  }

  @PostMapping("/intercambios")
  public ResponseEntity<String> publicarIntercambio(@PathVariable Integer userId, @PathVariable Integer numFigurita){
    publicacionesService.publicarIntercambioFigurita(userId, numFigurita);
    return ResponseEntity.ok().body("Publicacion de intercambio realizada");
  }

  //endpoint para ofrecer una propuesta de intercambio

}
