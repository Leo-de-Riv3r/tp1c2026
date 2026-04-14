package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import com.tacs.tp1c2026.services.AlertService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.entities.dto.output.SugerenciaIntercambioDto;
import com.tacs.tp1c2026.entities.dto.output.SugerenciasResponseDto;
import com.tacs.tp1c2026.entities.dto.output.UsuarioBasicoDto;
import com.tacs.tp1c2026.repositories.UsuariosRepository;

@RestController
@RequestMapping("/usuario")
public class UsuariosController {

  private final AlertService alertService;

  public UsuariosController(AlertService alertService) {
    this.alertService = alertService;
  }

  @GetMapping("/{userId}/alertas")
  public ResponseEntity<List<AlertaDto>> obtenerAlertas(@PathVariable Integer userId) {
    return ResponseEntity.ok(alertService.obtenerAlertasUsuario(userId));
  }

  @Autowired
  private UsuariosRepository usuariosRepository;

  @GetMapping("/{id}/sugerencias")
  public SugerenciasResponseDto obtenerSugerencias(@PathVariable Integer id) {
    Usuario usuario = usuariosRepository.findById(id).orElseThrow();

    List<SugerenciaIntercambioDto> sugerencias = usuario.getSugerenciasIntercambios()
        .stream()
        .map(sugerido -> mapearSugerencia(usuario, sugerido))
        .collect(Collectors.toList());

    return new SugerenciasResponseDto(sugerencias);
  }

  private SugerenciaIntercambioDto mapearSugerencia(Usuario usuarioActual, Usuario usuarioSugerido) {
    UsuarioBasicoDto usuarioDto = new UsuarioBasicoDto(usuarioSugerido.getId(), usuarioSugerido.getNombre());

    // Figuritas que el sugerido tiene (repetidas) y el usuario actual no tiene (faltantes)
    List<FiguritaRepetidaDto> figuritasQueTiene = usuarioSugerido.getRepetidas()
        .stream()
        .filter(rep -> usuarioActual.getFaltantes().stream()
            .anyMatch(falt -> falt.getId().equals(rep.getFigurita().getId())))
        .map(this::convertirFiguritaColeccionADto)
        .collect(Collectors.toList());

    // Figuritas que el usuario actual tiene (repetidas) y el sugerido no tiene (faltantes)
    List<FiguritaFaltanteDto> figuritasQueFaltan = usuarioActual.getRepetidas()
        .stream()
        .filter(rep -> usuarioSugerido.getFaltantes().stream()
            .anyMatch(falt -> falt.getId().equals(rep.getFigurita().getId())))
        .map(this::convertirFiguritaADto)
        .collect(Collectors.toList());

    return new SugerenciaIntercambioDto(usuarioDto, figuritasQueTiene, figuritasQueFaltan);
  }

  private FiguritaRepetidaDto convertirFiguritaColeccionADto(FiguritaColeccion coleccion) {
    FiguritaRepetidaDto dto = new FiguritaRepetidaDto();
    Figurita figurita = coleccion.getFigurita();
    dto.setNumero(figurita.getNumero());
    dto.setJugador(figurita.getJugador());
    dto.setSeleccion(figurita.getSeleccion());
    dto.setEquipo(figurita.getEquipo());
    dto.setDescripcion(figurita.getDescripcion());
    dto.setCategoria(figurita.getCategoria());
    dto.setCantidad(coleccion.getCantidad());
    dto.setTipoParticipacion(coleccion.getTipoParticipacion());
    return dto;
  }

  private FiguritaFaltanteDto convertirFiguritaADto(FiguritaColeccion coleccion) {
    FiguritaFaltanteDto dto = new FiguritaFaltanteDto();
    Figurita figurita = coleccion.getFigurita();
    dto.setNumero(figurita.getNumero());
    dto.setJugador(figurita.getJugador());
    dto.setSeleccion(figurita.getSeleccion());
    dto.setEquipo(figurita.getEquipo());
    dto.setDescripcion(figurita.getDescripcion());
    dto.setCategoria(figurita.getCategoria());
    return dto;
  }
}
