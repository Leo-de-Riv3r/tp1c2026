package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.input.InteresadoDto;
import com.tacs.tp1c2026.services.SubastaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subastas")
public class SubastaController {

  private final SubastaService subastaService;

  public SubastaController(SubastaService subastaService) {
    this.subastaService = subastaService;
  }

  @PostMapping("/{subastaId}/interesados")
  public ResponseEntity<String> agregarUsuarioInteresado(
      @PathVariable Integer subastaId,
      @RequestBody InteresadoDto dto) {
    subastaService.agregarUsuarioInteresado(subastaId, dto.getUserId());
    return ResponseEntity.ok("Usuario agregado como interesado");
  }
}
