package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import com.tacs.tp1c2026.repositories.PropuestasIntercambioRepository;
import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
import com.tacs.tp1c2026.repositories.SubastasRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/admin")
public class AdminController {

  private final UsuariosRepository usuariosRepository;
  private final SubastasRepository subastasRepository;
  private final PropuestasIntercambioRepository propuestasIntercambioRepository;
  private final PublicacionesIntercambioRepository publicacionesIntercambioRepository;

  public AdminController(
      UsuariosRepository usuariosRepository,
      SubastasRepository subastasRepository,
      PropuestasIntercambioRepository propuestasIntercambioRepository,
      PublicacionesIntercambioRepository publicacionesIntercambioRepository) {
    this.usuariosRepository = usuariosRepository;
    this.subastasRepository = subastasRepository;
    this.propuestasIntercambioRepository = propuestasIntercambioRepository;
    this.publicacionesIntercambioRepository = publicacionesIntercambioRepository;
  }

  /**
   * {@code GET /api/admin/usuarios_registrados} &mdash; Obtiene el total de usuarios registrados en el sistema.
   *
   * @return 200 OK con el número total de usuarios registrados
   */
  @GetMapping("/usuarios_registrados")
  public ResponseEntity<Long> getUsuariosRegistrados() {
    Long totalUsuarios = usuariosRepository.count();
    return ResponseEntity.ok(totalUsuarios);
  }

  /**
   * {@code GET /api/admin/figuritas_publicadas} &mdash; Obtiene el total de figuritas publicadas en intercambios.
   *
   * @return 200 OK con el número total de figuritas publicadas
   */
  @GetMapping("/figuritas_publicadas")
  public ResponseEntity<Long> getFiguritasPublicadas() {
    Long totalPublicaciones = publicacionesIntercambioRepository.count();
    return ResponseEntity.ok(totalPublicaciones);
  }

  /**
   * {@code GET /api/admin/propuestas_realizadas} &mdash; Obtiene el total de propuestas de intercambio realizadas.
   *
   * @return 200 OK con el número total de propuestas realizadas
   */
  @GetMapping("/propuestas_realizadas")
  public ResponseEntity<Long> getPropuestasRealizadas() {
    Long totalPropuestas = propuestasIntercambioRepository.count();
    return ResponseEntity.ok(totalPropuestas);
  }

  /**
   * {@code GET /api/admin/subastas_activas} &mdash; Obtiene el total de subastas activas en el sistema.
   *
   * @return 200 OK con el número total de subastas activas
   */
  @GetMapping("/subastas_activas")
  public ResponseEntity<Long> getSubastasActivas() {
    Long totalSubastasActivas =
        subastasRepository.findAll().stream()
            .filter(s -> s.getEstado() == EstadoSubasta.ACTIVA)
            .count();
    return ResponseEntity.ok(totalSubastasActivas);
  }

  /**
   * {@code GET /api/admin/intercambios_concretados} &mdash; Obtiene el total de intercambios concretados/finalizados.
   *
   * @return 200 OK con el número total de intercambios concretados
   */
  @GetMapping("/intercambios_concretados")
  public ResponseEntity<Long> getIntercambiosConcretados() {
    int totalIntercambiosConcretados =
        publicacionesIntercambioRepository.findByEstado(EstadoPublicacion.FINALIZADA).size();
    return ResponseEntity.ok((long) totalIntercambiosConcretados);
  }
}
