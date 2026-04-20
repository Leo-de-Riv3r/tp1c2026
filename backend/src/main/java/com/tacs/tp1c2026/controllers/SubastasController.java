// package com.tacs.tp1c2026.controllers;

// import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaDto;
// import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaOfertaDto;
// import com.tacs.tp1c2026.entities.dto.output.SubastaDto;
// import com.tacs.tp1c2026.services.SubastasService;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/api/subastas")
// public class SubastasController {
//   private final SubastasService subastasService;

//   public SubastasController(SubastasService subastasService) {
//     this.subastasService = subastasService;
//   }

//   /**
//    * {@code POST /api/subastas} &mdash; Crea una nueva subasta publicando una figurita repetida del usuario.
//    *
//    * @param userId          identificador del usuario que crea la subasta
//    * @param nuevaSubastaDto datos de la nueva subasta (figurita, duración y mínimo de figuritas)
//    * @return 200 OK con mensaje de confirmación indicando el ID de la subasta creada
//    */
//   @PostMapping("/create")
//   public ResponseEntity<String> crearSubasta(@RequestParam Integer userId, @RequestBody NuevaSubastaDto nuevaSubastaDto) {

//     Integer subastaId = subastasService.crearSubasta(userId, nuevaSubastaDto);
//     return ResponseEntity.ok("Subasta #"+subastaId+" creada con exito");

//   }

//   @GetMapping("/activas")
//   public ResponseEntity<List<SubastaDto>> obtenerSubastasActivasGlobales() {
//     return ResponseEntity.ok(subastasService.obtenerSubastasActivasGlobales());
//   }


//   /**
//    * {@code POST /api/subastas/{subastaId}/ofertar} &mdash; Registra una oferta sobre una subasta activa.
//    *
//    * @param subastaId              identificador de la subasta
//    * @param userId                 identificador del usuario postor
//    * @param nuevaSubastaOfertaDto  figuritas ofrecidas como parte de la oferta
//    * @return 200 OK con mensaje de confirmación
//    */
//   @PostMapping("/{subastaId}/ofertar")
//   public ResponseEntity<String> ofertarSubasta(@PathVariable Integer subastaId, @RequestParam Integer userId, @RequestBody NuevaSubastaOfertaDto nuevaSubastaOfertaDto) {

//     subastasService.ofertarSubasta(userId, subastaId, nuevaSubastaOfertaDto);
//     return ResponseEntity.ok("Oferta registrada exito");

//   }

//   /**
//    * {@code PUT /api/subastas/{subastaId}/ofertas/{ofertaId}/aceptar} &mdash;
//    * Acepta una oferta de subasta pendiente y adjudica la subasta al postor.
//    *
//    * @param subastaId identificador de la subasta
//    * @param ofertaId  identificador de la oferta a aceptar
//    * @param userId    identificador del usuario dueño de la subasta
//    * @return 200 OK con mensaje de confirmación
//    */
//   @PutMapping("/{subastaId}/ofertas/{ofertaId}/aceptar")
//   public ResponseEntity<String> aceptarOfertaSubasta(@PathVariable Integer subastaId, @PathVariable Integer ofertaId, @RequestParam Integer userId) {
//     subastasService.aceptarOfertaSubasta(userId, subastaId, ofertaId);
//     return ResponseEntity.ok("Oferta de subasta aceptada");
//   }

//   /**
//    * {@code PUT /api/subastas/{subastaId}/ofertas/{ofertaId}/rechazar} &mdash;
//    * Rechaza una oferta de subasta pendiente.
//    *Z
//    * @param subastaId identificador de la subasta
//    * @param ofertaId  identificador de la oferta a rechazar
//    * @param userId    identificador del usuario dueño de la subasta
//    * @return 200 OK con mensaje de confirmación
//    */
//   @PutMapping("/{subastaId}/ofertas/{ofertaId}/rechazar")
//   public ResponseEntity<String> rechazarOfertaSubasta(@PathVariable Integer subastaId, @PathVariable Integer ofertaId, @RequestParam Integer userId) {
//     subastasService.rechazarOfertaSubasta(userId, subastaId, ofertaId);
//     return ResponseEntity.ok("Oferta de subasta rechazada");
//   }
// }
