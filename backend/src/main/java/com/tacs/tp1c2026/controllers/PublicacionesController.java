package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.PropuestaIntercambio;
import com.tacs.tp1c2026.entities.dto.input.PropuestaIntercambioDto;
import com.tacs.tp1c2026.services.AlertService;
import com.tacs.tp1c2026.services.PublicacionesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionesController {
  private final PublicacionesService publicacionesService;
  private final AlertService alertService;

  public PublicacionesController(PublicacionesService publicacionesService, AlertService alertService) {
    this.publicacionesService = publicacionesService;
    this.alertService = alertService;
  }

  @PostMapping("/intercambios")
  public ResponseEntity<String> publicarIntercambio(@RequestParam Integer userId, @RequestParam Integer numFigurita){
    publicacionesService.publicarIntercambioFigurita(userId, numFigurita);
    return ResponseEntity.ok().body("Publicacion de intercambio realizada");
  }

  @PostMapping("/intercambios/{publicacionId}/propuestas")
  public ResponseEntity<String> ofrecerPropuestaIntercambio(@PathVariable Integer publicacionId, @RequestParam Integer userId, @RequestBody PropuestaIntercambioDto dto){
    PropuestaIntercambio propuesta = publicacionesService.ofrecerPropuestaIntercambio(publicacionId, userId, dto.getNumfiguritas());
    alertService.notificarPropuestaRecibida(propuesta);
    return ResponseEntity.ok().body("Propuesta de intercambio realizada");
  }

  @PutMapping("/intercambios/{publicacionId}/propuestas/{propuestaId}/rechazar")
  public ResponseEntity<String> rechazarPropuestaIntercambio(@PathVariable Integer publicacionId, @PathVariable Integer propuestaId, @PathVariable Integer userId){
    publicacionesService.rechazarPropuesta(publicacionId, propuestaId, userId);
    return ResponseEntity.ok().body("Propuesta de intercambio rechazada");
  }

}
