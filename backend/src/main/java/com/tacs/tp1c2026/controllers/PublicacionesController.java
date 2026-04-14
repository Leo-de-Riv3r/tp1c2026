package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.input.PropuestaIntercambioDto;
import com.tacs.tp1c2026.entities.dto.output.PaginacionDto;
import com.tacs.tp1c2026.entities.dto.output.PropuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.output.PublicacionDto;
import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.services.PublicacionesService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

  public PublicacionesController(PublicacionesService publicacionesService) {
    this.publicacionesService = publicacionesService;
  }

  @PostMapping("/intercambios")
  public ResponseEntity<String> publicarIntercambio(@RequestParam Integer userId, @RequestParam Integer numFigurita){
    publicacionesService.publicarIntercambioFigurita(userId, numFigurita);
    return ResponseEntity.ok().body("Publicacion de intercambio realizada");
  }

  @PostMapping("/intercambios/{publicacionId}/propuestas")
  public ResponseEntity<String> ofrecerPropuestaIntercambio(@PathVariable Integer publicacionId, @RequestParam Integer userId, @RequestBody PropuestaIntercambioDto dto){
    publicacionesService.ofrecerPropuestaIntercambio(publicacionId, userId, dto.getNumfiguritas());
    return ResponseEntity.ok().body("Propuesta de intercambio realizada");
  }

  @PutMapping("/intercambios/{publicacionId}/propuestas/{propuestaId}/rechazar")
  public ResponseEntity<String> rechazarPropuestaIntercambio(@PathVariable Integer publicacionId, @PathVariable Integer propuestaId, @RequestParam Integer userId){
    publicacionesService.rechazarPropuesta(publicacionId, propuestaId, userId);
    return ResponseEntity.ok().body("Propuesta de intercambio rechazada");
  }

  @GetMapping("/intercambios")
  public ResponseEntity<PaginacionDto<PublicacionDto>> buscarPublicaciones(
      @RequestParam(required = false) String seleccion,
      @RequestParam(required = false) String nombreJugador,
      @RequestParam(required = false) String equipo,
      @RequestParam(required = false) Categoria categoria,
      @RequestParam(required = false, defaultValue = "1") Integer page,
      @RequestParam(required = false, defaultValue = "10")Integer per_page
      ) {
    PaginacionDto<PublicacionDto> resultadoBusqueda = publicacionesService.buscarPublicaciones(
        seleccion, nombreJugador, equipo, categoria, page, per_page);
    return ResponseEntity.ok().body(resultadoBusqueda);
  }

  @GetMapping("/propuestas")
  public ResponseEntity<List<PropuestaRecibidaDto>> obtenerPropuestasRecibidas(@RequestParam Integer userId) {
    return ResponseEntity.ok().body(publicacionesService.obtenerPropuestasRecibidas(userId));
  }

}
