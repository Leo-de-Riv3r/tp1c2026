package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.services.PerfilService;
import com.tacs.tp1c2026.services.FiguritasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/figuritas")
public class FiguritasController {
  private final FiguritasService figuritasService;
  private final PerfilService perfilService;

  public FiguritasController(FiguritasService figuritasService, PerfilService perfilService) {
    this.figuritasService = figuritasService;
    this.perfilService = perfilService;
  }

  /**
   * {@code POST /api/figuritas/registrar-repetida} &mdash; Registra una figurita como repetida
  * en la colección del usuario y actualiza su asignación de perfiles.
   *
   * @param figuritaRepetidaDto datos de la figurita repetida a registrar
   * @param userId              identificador del usuario
   * @return 200 OK con mensaje de confirmación
   */
  @PostMapping("/registrar-repetida")
  public ResponseEntity<String> agregarFigurita(@RequestBody FiguritaRepetidaDto figuritaRepetidaDto, @RequestParam Integer userId) {
    figuritasService.registrarFiguritaRepetida(figuritaRepetidaDto, userId);
    perfilService.actualizarPerfilesUsuario(userId);
    return ResponseEntity.ok().body("Figurita repetida agregada");
  }


  /**
   * {@code POST /api/figuritas/registrar-faltante} &mdash; Registra una figurita como faltante
  * en la colección del usuario y actualiza su asignación de perfiles.
   *
   * @param figuritaFaltanteDto datos de la figurita faltante a registrar
   * @param userId              identificador del usuario
   * @return 200 OK con mensaje de confirmación
   */
  @PostMapping("/registrar-faltante")
  public ResponseEntity<String> registrarFiguritaFaltante(@RequestBody FiguritaFaltanteDto figuritaFaltanteDto, @RequestParam Integer userId) {
    figuritasService.registrarFiguritaFaltante(figuritaFaltanteDto, userId);
    perfilService.actualizarPerfilesUsuario(userId);
    return ResponseEntity.ok().body("Figurita faltante registrada");
  }


}