package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.services.FiguritasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/figuritas")
public class FiguritasController {
  private final FiguritasService figuritasService;

  public FiguritasController(FiguritasService figuritasService) {
    this.figuritasService = figuritasService;
  }

  @PostMapping("/registrar-repetida")
  public ResponseEntity<String> agregarFigurita(@RequestBody FiguritaRepetidaDto figuritaRepetidaDto, @RequestParam Integer userId) {
    figuritasService.registrarFiguritaRepetida(figuritaRepetidaDto, userId);
    return ResponseEntity.ok().body("Figurita repetida agregada");
  }


  @PostMapping("/registrar-faltante")
  public ResponseEntity<String> registrarFiguritaFaltante(@RequestBody FiguritaFaltanteDto figuritaFaltanteDto, @RequestParam Integer userId) {
    figuritasService.registrarFiguritaFaltante(figuritaFaltanteDto, userId);
    return ResponseEntity.ok().body("Figurita faltante registrada");
  }


}