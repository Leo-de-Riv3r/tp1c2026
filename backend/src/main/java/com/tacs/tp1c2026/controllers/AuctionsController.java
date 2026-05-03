 package com.tacs.tp1c2026.controllers;

 import com.tacs.tp1c2026.entities.dto.input.NewAuctionDto;
 import com.tacs.tp1c2026.entities.dto.input.NewAuctionOfferDto;
 import com.tacs.tp1c2026.entities.dto.input.SearchPublicationsFilters;
 import com.tacs.tp1c2026.entities.dto.output.AuctionDto;
 import com.tacs.tp1c2026.entities.dto.output.PaginationDtoOutput;
 import com.tacs.tp1c2026.entities.enums.CardCategory;
 import com.tacs.tp1c2026.services.AuctionsService;
 import jakarta.validation.Valid;
 import java.time.LocalDateTime;
 import java.util.Map;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;

 @RestController
 @RequestMapping("/auctions")
 public class AuctionsController {
   private final AuctionsService auctionsService;

   public AuctionsController(AuctionsService auctionsService) {
     this.auctionsService = auctionsService;
   }

   /**
    * {@code POST /api/subastas} &mdash; Crea una nueva subasta publicando una card repetida del usuario.
    * @return 200 OK con mensaje de confirmación indicando el ID de la subasta creada
    */
   @GetMapping("/{auctionId}")
   public ResponseEntity<AuctionDto> getAuction(
       @PathVariable String auctionId
   ) {
     return ResponseEntity.ok(auctionsService.getAuctionDto(auctionId));
   }

   @PostMapping
   public ResponseEntity<Map<String, Object>> createAuction(
       @RequestAttribute("userId") String userId, @Valid @RequestBody NewAuctionDto dto) {
     String subastaId = auctionsService.createAuction(userId, dto);
     Map<String, Object> body = Map.of(
         "timestamp", LocalDateTime.now(),
         "message", "Subasta creada con exito",
         "auctionId", subastaId
     );
     return ResponseEntity.ok().body(body);
   }

   @PostMapping("/{auctionId}/offers")
   public ResponseEntity<Map<String, Object>> ofertarSubasta(@PathVariable String auctionId,
   @RequestAttribute("userId") String userId, @Valid @RequestBody NewAuctionOfferDto dto) {
     dto.setAuctionId(auctionId);
     auctionsService.offerProposalAuction(userId, dto);
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
     return ResponseEntity.ok(auctionsService.getMyAuctions(userId, page, per_page));
   }

   @GetMapping
   public ResponseEntity<PaginationDtoOutput<AuctionDto>> searchActiveAuction(
       @RequestParam(defaultValue = "0") Integer page,
       @RequestParam(defaultValue = "10") Integer per_page,
       @RequestParam(required = false) String name,
       @RequestParam(required = false) String country,
       @RequestParam(required = false) String team,
       @RequestParam(required = false) CardCategory category
   ){
     SearchPublicationsFilters filters = new SearchPublicationsFilters(name, country, team, category);
     PaginationDtoOutput<AuctionDto> result = auctionsService.searchActiveAuctions(page, per_page, filters);
     return ResponseEntity.ok(result);
   }

 }