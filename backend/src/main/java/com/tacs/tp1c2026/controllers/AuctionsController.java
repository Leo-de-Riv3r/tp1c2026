 package com.tacs.tp1c2026.controllers;

 import com.tacs.tp1c2026.entities.dto.input.NewAuctionDTO;
 import com.tacs.tp1c2026.entities.dto.input.NewAuctionOfferDTO;
 import com.tacs.tp1c2026.entities.dto.output.AuctionDTO;
 import com.tacs.tp1c2026.services.AuctionsService;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;

 import java.util.List;

 @RestController
 @RequestMapping("/api/subastas")
 public class AuctionsController {
   private final AuctionsService auctionsService;

   public AuctionsController(AuctionsService auctionsService) {
     this.auctionsService = auctionsService;
   }

   /**
    * {@code POST /api/subastas} &mdash; Crea una nueva subasta publicando una figurita repetida del usuario.
    *
    * @param userId          identificador del usuario que crea la subasta
    * @param newAuctionDTO datos de la nueva subasta (figurita, duración y mínimo de figuritas)
    * @return 200 OK con mensaje de confirmación indicando el ID de la subasta creada
    */
   @PostMapping("/create")
   public ResponseEntity<String> createAuction(@RequestParam Integer userId, @RequestBody NewAuctionDTO newAuctionDTO) {

     Integer subastaId = auctionsService.createAuction(userId, newAuctionDTO);
     return ResponseEntity.ok("Subasta #"+subastaId+" creada con exito");

   }

   @GetMapping("/activas")
   public ResponseEntity<List<AuctionDTO>> getAllAuctionsActive() {
     return ResponseEntity.ok(auctionsService.getAllAuctionsActive());
   }


   /**
    * {@code POST /api/subastas/{subastaId}/ofertar} &mdash; Registra una oferta sobre una subasta activa.
    *
    * @param subastaId              identificador de la subasta
    * @param userId                 identificador del usuario postor
    * @param newAuctionOfferDTO  figuritas ofrecidas como parte de la oferta
    * @return 200 OK con mensaje de confirmación
    */
   @PostMapping("/{subastaId}/ofertar")
   public ResponseEntity<String> placeAuctionOffer(@PathVariable Integer subastaId, @RequestParam Integer userId, @RequestBody NewAuctionOfferDTO newAuctionOfferDTO) {

     auctionsService.placeAuctionOffer(userId, subastaId, newAuctionOfferDTO);
     return ResponseEntity.ok("Oferta registrada exito");

   }

   /**
    * {@code PUT /api/subastas/{subastaId}/ofertas/{ofertaId}/aceptar} &mdash;
    * Acepta una oferta de subasta pendiente y adjudica la subasta al postor.
    *
    * @param subastaId identificador de la subasta
    * @param ofertaId  identificador de la oferta a aceptar
    * @param userId    identificador del usuario dueño de la subasta
    * @return 200 OK con mensaje de confirmación
    */
   @PutMapping("/{subastaId}/ofertas/{ofertaId}/aceptar")
   public ResponseEntity<String> acceptAuctionOffer(@PathVariable Integer subastaId, @PathVariable Integer ofertaId, @RequestParam Integer userId) {
     auctionsService.acceptAuctionOffer(userId, subastaId, ofertaId);
     return ResponseEntity.ok("Oferta de subasta aceptada");
   }

   /**
    * {@code PUT /api/subastas/{subastaId}/ofertas/{ofertaId}/rechazar} &mdash;
    * Rechaza una oferta de subasta pendiente.
    *Z
    * @param subastaId identificador de la subasta
    * @param ofertaId  identificador de la oferta a rechazar
    * @param userId    identificador del usuario dueño de la subasta
    * @return 200 OK con mensaje de confirmación
    */
   @PutMapping("/{subastaId}/ofertas/{ofertaId}/rechazar")
   public ResponseEntity<String> rejectAuctionOffer(@PathVariable Integer subastaId, @PathVariable Integer ofertaId, @RequestParam Integer userId) {
     auctionsService.rejectAuctionOffer(userId, subastaId, ofertaId);
     return ResponseEntity.ok("Oferta de subasta rechazada");
   }
 }
