 package com.tacs.tp1c2026.controllers;

 import com.tacs.tp1c2026.entities.dto.input.NewExchangeProposalDto;
 import com.tacs.tp1c2026.entities.dto.input.NewExchangePublicationDto;
 import com.tacs.tp1c2026.entities.dto.input.ReviewProposalDto;
 import com.tacs.tp1c2026.entities.dto.input.SearchPublicationsFilters;
 import com.tacs.tp1c2026.entities.dto.output.ExchangeProposalDto;
 import com.tacs.tp1c2026.entities.dto.output.ExchangePublicationDto;
 import com.tacs.tp1c2026.entities.dto.output.PaginationDtoOutput;
 import com.tacs.tp1c2026.entities.enums.CardCategory;
 import com.tacs.tp1c2026.entities.enums.ProposalState;
 import com.tacs.tp1c2026.services.PublicationsService;
 import jakarta.validation.Valid;
 import java.time.LocalDateTime;
 import java.util.Map;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.PostMapping;
 import org.springframework.web.bind.annotation.PutMapping;
 import org.springframework.web.bind.annotation.RequestAttribute;
 import org.springframework.web.bind.annotation.RequestBody;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.RestController;

 @RestController
 @RequestMapping("/exchanges")
 public class ExchangesController {
   private final PublicationsService publicacionesService;

   public ExchangesController(PublicationsService publicacionesService) {
     this.publicacionesService = publicacionesService;
   }

   /**
    * {@code POST /api/publicaciones/intercambios} &mdash; Publica una card repetida del usuario para intercambio.
    *
    * @param userId      identificador del usuario que realiza la publicación
    * @return 200 OK con mensaje de confirmación
    */
   @PostMapping("/")
   public ResponseEntity<Map<String, Object>> publishExchange(
       @Valid @RequestBody NewExchangePublicationDto dto,
       @RequestAttribute("userId") String userId) {
     String publicationId = publicacionesService.publishCardExchange(dto, userId);
     Map<String, Object> body = Map.of(
         "timestamp", LocalDateTime.now(),
         "message", "Publicación de intercambio realizada con éxito",
         "publicationId", publicationId
     );
     return ResponseEntity.ok().body(body);
   }

   /**
    * {@code POST /api/publicaciones/intercambios/{publicacionId}/propuestas} &mdash;
    * Envía una propuesta de intercambio sobre una publicación existente.
    *
    * @param userId        identificador del usuario que propone el intercambio
    * @param dto           lista de números de card ofrecidos como contraprestación
    * @return 200 OK con mensaje de confirmación
    */
   @PostMapping("/{publicationId}/proposals")
   public ResponseEntity<Map<String, Object>> offerExchangeProposal(
       @PathVariable String publicationId,
       @RequestAttribute("userId") String userId,
       @Valid @RequestBody NewExchangeProposalDto dto){
     dto.setPublicationId(publicationId);
     publicacionesService.offerProposalExchange(userId, dto);
     Map<String, Object> body = Map.of(
         "timestamp", LocalDateTime.now(),
         "message", "Propuesta de intercambio enviada con éxito"
     );
     return ResponseEntity.ok().body(body);
   }

   @GetMapping("/{publicationId}/proposals")
   public ResponseEntity<PaginationDtoOutput<ExchangeProposalDto>> getReceivedProposalsByPublication(
       @PathVariable String publicationId,
       @RequestAttribute("userId") String userId,
       @RequestParam(defaultValue = "1") Integer page,
       @RequestParam(defaultValue = "10") Integer per_page
   ) {
     PaginationDtoOutput<ExchangeProposalDto> result = publicacionesService.getReceivedProposalsByPublication(userId, publicationId, page, per_page);
     return ResponseEntity.ok(result);
   }

   @GetMapping("/offeredProposals")
   public ResponseEntity<PaginationDtoOutput<ExchangeProposalDto>> getOfferedProposals(
       @RequestAttribute("userId") String userId,
       @RequestParam(defaultValue = "1") Integer page,
       @RequestParam(defaultValue = "10") Integer per_page,
       @RequestParam(defaultValue = "PENDIENTE") ProposalState state
   ) {
     PaginationDtoOutput<ExchangeProposalDto> result = publicacionesService.getOfferedProposals(userId, state, page, per_page);
     return ResponseEntity.ok(result);
   }

   @GetMapping("/receivedProposals")
   public ResponseEntity<PaginationDtoOutput<ExchangeProposalDto>> getReceivedProposals(
       @RequestAttribute("userId") String userId,
       @RequestParam(defaultValue = "1") Integer page,
       @RequestParam(defaultValue = "10") Integer per_page,
       @RequestParam(defaultValue = "PENDIENTE") ProposalState state
   ) {
     PaginationDtoOutput<ExchangeProposalDto> result = publicacionesService.getReceivedProposals(userId, state, page, per_page);
     return ResponseEntity.ok(result);
   }

       /**
    * {@code PUT /api/publicaciones/intercambios/{publicacionId}/propuestas/{propuestaId}/aceptar} &mdash;
    * Acepta una propuesta de intercambio pendiente.
    *
    * @param userId identificador del usuario dueño de la publicación
    * @return 200 OK con mensaje de confirmación
    */
   @PutMapping("/{publicationId}/proposals/{proposalId}")
   public ResponseEntity<Map<String, Object>> reviewProposal(
       @PathVariable String publicationId,
       @PathVariable String proposalId,
       @RequestAttribute("userId") String userId,
       @Valid @RequestBody ReviewProposalDto dto
   ){
     dto.setPublicationId(publicationId);
     dto.setProposalId(proposalId);
     publicacionesService.reviewProposal(dto, userId);

     Map<String, Object> body = Map.of(
         "timestamp", LocalDateTime.now(),
         "message", "Propuesta de intercambio revisada con éxito"
     );

     return ResponseEntity.ok().body(body);
   }

   @GetMapping("/")
   public ResponseEntity<PaginationDtoOutput<ExchangePublicationDto>> searchActivePublications(
       @RequestParam(defaultValue = "1") Integer page,
       @RequestParam(defaultValue = "10") Integer per_page,
       @RequestParam(required = false) String name,
       @RequestParam(required = false) String country,
       @RequestParam(required = false) String team,
       @RequestParam(required = false) CardCategory category
   ) {
     SearchPublicationsFilters filters = new SearchPublicationsFilters(name, country, team, category);
     PaginationDtoOutput<ExchangePublicationDto> result = publicacionesService.searchPublications(filters, page, per_page);
     return ResponseEntity.ok(result);
   }

   @GetMapping("/createdByMe")
   public ResponseEntity<PaginationDtoOutput<ExchangePublicationDto>> getPublicationsCreatedByUser(
       @RequestAttribute("userId") String userId,
       @RequestParam(defaultValue = "1") Integer page,
       @RequestParam(defaultValue = "10") Integer per_page
   ) {
     PaginationDtoOutput<ExchangePublicationDto> result = publicacionesService.getPublicationsCreatedByUser(userId, page, per_page);
     return ResponseEntity.ok(result);
   }
 }
