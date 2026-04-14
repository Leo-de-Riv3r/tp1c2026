package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaDto;
import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaOfertaDto;
import com.tacs.tp1c2026.services.SubastasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subastas")
public class SubastasController {
  private final SubastasService subastasService;

  public SubastasController(SubastasService subastasService) {
    this.subastasService = subastasService;
  }

  @PostMapping
  public ResponseEntity<String> crearSubasta(@RequestParam Integer userId, @RequestBody NuevaSubastaDto nuevaSubastaDto) {

    Integer subastaId = subastasService.crearSubasta(userId, nuevaSubastaDto);
    return ResponseEntity.ok("Subasta #"+subastaId+" creada con exito");

  }

  @PostMapping("/{subastaId}/ofertas")
  public ResponseEntity<String> ofertarSubasta(@PathVariable Integer subastaId, @RequestParam Integer userId, @RequestBody NuevaSubastaOfertaDto nuevaSubastaOfertaDto) {

    subastasService.ofertarSubasta(userId, subastaId, nuevaSubastaOfertaDto);
    return ResponseEntity.ok("Oferta registrada exito");

  }

  @PutMapping("/{subastaId}/ofertas/{ofertaId}/aceptar")
  public ResponseEntity<String> aceptarOfertaSubasta(@PathVariable Integer subastaId, @PathVariable Integer ofertaId, @RequestParam Integer userId) {
    subastasService.aceptarOfertaSubasta(userId, subastaId, ofertaId);
    return ResponseEntity.ok("Oferta de subasta aceptada");
  }

  @PutMapping("/{subastaId}/ofertas/{ofertaId}/rechazar")
  public ResponseEntity<String> rechazarOfertaSubasta(@PathVariable Integer subastaId, @PathVariable Integer ofertaId, @RequestParam Integer userId) {
    subastasService.rechazarOfertaSubasta(userId, subastaId, ofertaId);
    return ResponseEntity.ok("Oferta de subasta rechazada");
  }
}

