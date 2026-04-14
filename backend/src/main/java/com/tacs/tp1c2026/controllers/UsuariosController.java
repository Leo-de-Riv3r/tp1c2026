package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import com.tacs.tp1c2026.services.AlertService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

  private final AlertService alertService;

  public UsuariosController(AlertService alertService) {
    this.alertService = alertService;
  }

  @GetMapping("/{userId}/alertas")
  public ResponseEntity<List<AlertaDto>> obtenerAlertas(@PathVariable Integer userId) {
    return ResponseEntity.ok(alertService.obtenerAlertasUsuario(userId));
  }
}
