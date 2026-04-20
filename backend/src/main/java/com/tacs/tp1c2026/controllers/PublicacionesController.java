// package com.tacs.tp1c2026.controllers;

// import com.tacs.tp1c2026.entities.dto.input.PropuestaIntercambioDto;
// import com.tacs.tp1c2026.services.PublicacionesService;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.PutMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;

// @RestController
// @RequestMapping("/api/publicaciones")
// public class PublicacionesController {
//   private final PublicacionesService publicacionesService;

//   public PublicacionesController(PublicacionesService publicacionesService) {
//     this.publicacionesService = publicacionesService;
//   }

//   /**
//    * {@code POST /api/publicaciones/intercambios} &mdash; Publica una figurita repetida del usuario para intercambio.
//    *
//    * @param userId      identificador del usuario que realiza la publicación
//    * @param numFigurita número de la figurita repetida a publicar
//    * @return 200 OK con mensaje de confirmación
//    */
//   @PostMapping("/intercambios")
//   public ResponseEntity<String> publicarIntercambio(@RequestParam Integer userId, @RequestParam Integer numFigurita){
//     publicacionesService.publicarIntercambioFigurita(userId, numFigurita);
//     return ResponseEntity.ok().body("Publicacion de intercambio realizada");
//   }

//   /**
//    * {@code POST /api/publicaciones/intercambios/{publicacionId}/propuestas} &mdash;
//    * Envía una propuesta de intercambio sobre una publicación existente.
//    *
//    * @param publicacionId identificador de la publicación objetivo
//    * @param userId        identificador del usuario que propone el intercambio
//    * @param dto           lista de números de figurita ofrecidos como contraprestación
//    * @return 200 OK con mensaje de confirmación
//    */
//   @PostMapping("/intercambios/{publicacionId}/propuestas")
//   public ResponseEntity<String> ofrecerPropuestaIntercambio(@PathVariable Integer publicacionId, @RequestParam Integer userId, @RequestBody PropuestaIntercambioDto dto){
//     publicacionesService.ofrecerPropuestaIntercambio(userId, publicacionId, dto.getNumfiguritas());
//     return ResponseEntity.ok().body("Propuesta de intercambio realizada");
//   }

//   /**
//    * {@code PUT /api/publicaciones/intercambios/{publicacionId}/propuestas/{propuestaId}/aceptar} &mdash;
//    * Acepta una propuesta de intercambio pendiente.
//    *
//    * @param publicacionId identificador de la publicación de intercambio
//    * @param propuestaId   identificador de la propuesta a aceptar
//    * @param userId        identificador del usuario dueño de la publicación
//    * @return 200 OK con mensaje de confirmación
//    */
//   @PutMapping("/intercambios/{publicacionId}/propuestas/{propuestaId}/aceptar")
//   public ResponseEntity<String> aceptarPropuestaIntercambio(@PathVariable Integer publicacionId, @PathVariable Integer propuestaId, @RequestParam Integer userId){
//     publicacionesService.aceptarPropuesta(publicacionId, propuestaId, userId);
//     return ResponseEntity.ok().body("Propuesta de intercambio aceptada");
//   }

//   /**
//    * {@code PUT /api/publicaciones/intercambios/{publicacionId}/propuestas/{propuestaId}/rechazar} &mdash;
//    * Rechaza una propuesta de intercambio pendiente.
//    *
//    * @param publicacionId identificador de la publicación de intercambio
//    * @param propuestaId   identificador de la propuesta a rechazar
//    * @param userId        identificador del usuario dueño de la publicación
//    * @return 200 OK con mensaje de confirmación
//    */
//   @PutMapping("/intercambios/{publicacionId}/propuestas/{propuestaId}/rechazar")
//   public ResponseEntity<String> rechazarPropuestaIntercambio(@PathVariable Integer publicacionId, @PathVariable Integer propuestaId, @RequestParam Integer userId){
//     publicacionesService.rechazarPropuesta(publicacionId, propuestaId, userId);
//     return ResponseEntity.ok().body("Propuesta de intercambio rechazada");
//   }
// }
