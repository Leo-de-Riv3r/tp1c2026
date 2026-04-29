 package com.tacs.tp1c2026.controllers;

 import com.tacs.tp1c2026.entities.dto.input.NewAuctionDto;
 import com.tacs.tp1c2026.entities.dto.input.NewAuctionOfferDto;
 import com.tacs.tp1c2026.entities.dto.output.AuctionDto;
 import com.tacs.tp1c2026.entities.dto.output.PaginationDtoOutput;
 import com.tacs.tp1c2026.entities.dto.output.SubastaDto;
 import com.tacs.tp1c2026.services.AuctionsService;
 import jakarta.validation.Valid;
 import java.time.LocalDateTime;
 import java.util.Map;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;

 import java.util.List;

 @RestController
 @RequestMapping("/auctions")
 public class AuctionsController {
   private final AuctionsService subastasService;

   public AuctionsController(AuctionsService subastasService) {
     this.subastasService = subastasService;
   }

   /**
    * {@code POST /api/subastas} &mdash; Crea una nueva subasta publicando una card repetida del usuario.
    *
    * @param userId          identificador del usuario que crea la subasta
    * @param dto datos de la nueva subasta (card, duración y mínimo de figuritas)
    * @return 200 OK con mensaje de confirmación indicando el ID de la subasta creada
    */
   @PostMapping("/create")
   public ResponseEntity<Map<String, Object>> createAuction(
       @RequestAttribute("userId") String userId, @Valid @RequestBody NewAuctionDto dto) {
     String subastaId = subastasService.createAuction(userId, dto);
     Map<String, Object> body = Map.of(
         "timestamp", LocalDateTime.now(),
         "message", "Figurita eliminada de coleccion con exito",
         "auctionId", subastaId
     );
     return ResponseEntity.ok().body(body);
   }

   @GetMapping("/active")
   public ResponseEntity<List<SubastaDto>> obtenerSubastasActivasGlobales() {
     return ResponseEntity.ok(subastasService.obtenerSubastasActivasGlobales());
   }

   @PostMapping("/{auctionId}/offer")
   public ResponseEntity<Map<String, Object>> ofertarSubasta(@PathVariable String auctionId,
   @RequestAttribute("userId") String userId, @Valid @RequestBody NewAuctionOfferDto dto) {
     dto.setAuctionId(auctionId);
     subastasService.offerProposalAuction(userId, dto);
     Map<String, Object> body = Map.of(
         "timestamp", LocalDateTime.now(),
         "message", "Oferta publicada con exito"
     );
     return ResponseEntity.ok().body(body);
   }

   @GetMapping("/createdByMe")
   public ResponseEntity<PaginationDtoOutput<AuctionDto>> getAuctionsCreatedByMe(
       @RequestAttribute("userId") String userId,
       @RequestParam(defaultValue = "0") Integer page,
       @RequestParam(defaultValue = "10") Integer per_page
   ) {
     return ResponseEntity.ok(subastasService.getMyAuctions(userId, page, per_page));
   }

 }