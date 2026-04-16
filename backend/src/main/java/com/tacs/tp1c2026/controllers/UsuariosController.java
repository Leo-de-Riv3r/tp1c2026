package com.tacs.tp1c2026.controllers;


import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.UserDto;
import com.tacs.tp1c2026.entities.dto.output.*;

import java.util.List;
import com.tacs.tp1c2026.services.UsuariosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {
    
    private final UsuariosService usuariosService;

    public UsuariosController(UsuariosService usuariosService) {
        this.usuariosService = usuariosService;
    }


    // DSP BORRAR
    @PostMapping("/create")
    public ResponseEntity<UsuarioDto> crearUsuario(@RequestBody UserDto dto) {
        // El usuario devuelto por el service YA DEBE tener el ID
        Usuario usuarioPersistido = usuariosService.crearUsuario(dto.getNombre());

        UsuarioDto response = new UsuarioDto();
        response.setId(usuarioPersistido.getId());
        response.setNombre(usuarioPersistido.getNombre());
        response.setFechaAlta(usuarioPersistido.getFechaAlta());

        return ResponseEntity.ok(response);
    }

    // DSP BORRAR
    @GetMapping
    public ResponseEntity<List<UsuarioDto>> listarUsuarios() {
        return ResponseEntity.ok(usuariosService.listarUsuarios());
    }

    /**
     * {@code GET /api/usuarios/{userId}/publicacionesIntercambio} &mdash; Retorna las publicaciones de intercambio creadas por el usuario.
     * @param userId identificador del usuario
     * @return 200 OK con la lista de publicaciones de intercambio del usuario
     */
    @GetMapping("/{userId}/publicacionesIntercambio")
    public ResponseEntity<List<PublicacionIntercambioDto>> obtenerPublicacionesPropias(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerPublicacionesPropias(userId));
    }


    // envio las propuestas de INTERCAMBIO de figus ENVIADAS por el usuario
    /**
     * {@code GET /api/usuarios/{userId}/propuestas/enviadas} &mdash; Retorna las propuestas de
     * intercambio enviadas por el usuario.
     *
     * @param userId identificador del usuario
     * @return 200 OK con la lista de propuestas enviadas
     */
    @GetMapping("/{userId}/propuestas/enviadas")
    public ResponseEntity<List<PropuestaIntercambioDto>> obtenerPropuestasEnviadas(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerPropuestasEnviadas(userId));
    }


    // envio las propuestas de INTERCAMBIO de figus RECIBIDAS por el usuario
    /**
     * {@code GET /api/usuarios/{userId}/propuestas/recibidas} &mdash; Retorna las propuestas de
     * intercambio recibidas en las publicaciones del usuario.
     *
     * @param userId identificador del usuario
     * @return 200 OK con la lista de propuestas recibidas
     */
    @GetMapping("/{userId}/propuestas/recibidas")
    public ResponseEntity<List<PropuestaIntercambioDto>> obtenerPropuestasRecibidas(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerPropuestasRecibidas(userId));
    }


//    // envios las SUBASTAS activas
//    /**
//     * {@code GET /api/usuarios/{userId}/subastas/activas} &mdash; Retorna las subastas activas
//     * del usuario.
//     *
//     * @param userId identificador del usuario
//     * @return 200 OK con la lista de subastas activas
//     */
//    @GetMapping("/{userId}/subastas/activas")
//    public ResponseEntity<List<SubastaDto>> obtenerSubastasActivas(@PathVariable Integer userId) {
//        return ResponseEntity.ok(usuariosService.obtenerSubastasActivas(userId));
//    }


    // envio todas las subastas que hice segun un estado, Si estado (ACTIVA,VENCIDA,ETC) no se envia, devuelve todas las subastas del usuario publicante
    /**
     * {@code GET /api/usuarios/{userId}/subastas} &mdash; Retorna las subastas del usuario
     * filtradas por estado. Si no se envía el parámetro {@code estado}, devuelve todas.
     *
     * @param userId identificador del usuario
     * @param estado estado de la subasta a filtrar (ACTIVA, VENCIDA, etc.); opcional
     * @return 200 OK con la lista de subastas filtradas
     */
    @GetMapping("/{userId}/subastas")
    public ResponseEntity<List<SubastaDto>> obtenerSubastasPorEstado(@PathVariable Integer userId, @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(usuariosService.obtenerSubastasPorEstado(userId, estado));
    }


    // envio las ofertas de subastas que realicé
    /**
     * {@code GET /api/usuarios/{userId}/subastas/ofertas-enviadas} &mdash; Retorna las ofertas de
     * subasta enviadas por el usuario.
     *
     * @param userId identificador del usuario postor
     * @return 200 OK con la lista de ofertas enviadas
     */
    @GetMapping("/{userId}/subastas/ofertas-enviadas")
    public ResponseEntity<List<OfertaSubastaDto>> obtenerOfertasSubastaEnviadas(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerOfertasSubastaEnviadas(userId));
    }


    // envio las ofertas de subastas que recibi
    /**
     * {@code GET /api/usuarios/{userId}/subastas/ofertas-recibidas} &mdash; Retorna las ofertas de
     * subasta recibidas en las subastas del usuario.
     *
     * @param userId identificador del usuario publicante
     * @return 200 OK con la lista de ofertas recibidas
     */
    @GetMapping("/{userId}/subastas/ofertas-recibidas")
    public ResponseEntity<List<OfertaSubastaDto>> obtenerOfertasSubastaRecibidas(@PathVariable Integer userId) {
        return ResponseEntity.ok(usuariosService.obtenerOfertasSubastaRecibidas(userId));
    }
}
