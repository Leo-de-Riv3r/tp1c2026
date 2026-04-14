package com.tacs.tp1c2026.controllers;


import com.tacs.tp1c2026.entities.dto.output.OfertaSubastaDto;
import com.tacs.tp1c2026.entities.dto.output.PropuestaIntercambioDto;
import com.tacs.tp1c2026.entities.dto.output.PublicacionIntercambioDto;
import com.tacs.tp1c2026.entities.dto.output.SubastaDto;
import java.util.List;
import com.tacs.tp1c2026.services.UsuariosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {
    
    private final UsuariosService usuariosService;

    public UsuariosController(UsuariosService usuariosService) {
        this.usuariosService = usuariosService;
    }


//    @GetMapping("/{userId}/operaciones")
//    public ResponseEntity<OperacionesUsuarioDto> obtenerOperaciones(@PathVariable Integer userId) {
//        return ResponseEntity.ok(usuariosService.obtenerOperaciones(userId));
//    }


    // envio las publicaciones de INTERCAMBIO de figus PROPIAS del usuario
    @GetMapping("/{userId}/publicaciones")
    public ResponseEntity<List<PublicacionIntercambioDto>> obtenerPublicacionesPropias(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerPublicacionesPropias(userId));
    }


    // envio las propuestas de INTERCAMBIO de figus ENVIADAS por el usuario
    @GetMapping("/{userId}/propuestas/enviadas")
    public ResponseEntity<List<PropuestaIntercambioDto>> obtenerPropuestasEnviadas(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerPropuestasEnviadas(userId));
    }


    // envio las propuestas de INTERCAMBIO de figus RECIBIDAS por el usuario
    @GetMapping("/{userId}/propuestas/recibidas")
    public ResponseEntity<List<PropuestaIntercambioDto>> obtenerPropuestasRecibidas(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerPropuestasRecibidas(userId));
    }


    // envios las SUBASTAS activas
    @GetMapping("/{userId}/subastas/activas")
    public ResponseEntity<List<SubastaDto>> obtenerSubastasActivas(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerSubastasActivas(userId));
    }


    // envio todas las subastas que hice segun un estado, Si estado (ACTIVA,VENCIDA,ETC) no se envia, devuelve todas las subastas del usuario publicante
    @GetMapping("/{userId}/subastas")
    public ResponseEntity<List<SubastaDto>> obtenerSubastasPorEstado(@PathVariable Integer userId, @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(usuariosService.obtenerSubastasPorEstado(userId, estado));
    }


    // envio las ofertas de subastas que realicé
    @GetMapping("/{userId}/subastas/ofertas-enviadas")
    public ResponseEntity<List<OfertaSubastaDto>> obtenerOfertasSubastaEnviadas(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerOfertasSubastaEnviadas(userId));
    }


    // envio las ofertas de subastas que recibi
    @GetMapping("/{userId}/subastas/ofertas-recibidas")
    public ResponseEntity<List<OfertaSubastaDto>> obtenerOfertasSubastaRecibidas(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerOfertasSubastaRecibidas(userId));
    }
}
