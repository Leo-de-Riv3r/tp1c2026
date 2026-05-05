package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.feedback.input.NewFeedbackDto;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradeProposalDTO;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradePublicationDto;
import com.tacs.tp1c2026.entities.dto.trade.input.ReviewProposalDto;
import com.tacs.tp1c2026.entities.dto.trade.output.TradePublicationDto;
import com.tacs.tp1c2026.entities.enums.CardType;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.services.PublicationService;
import com.tacs.tp1c2026.services.mappers.TradeMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/exchanges")
public class ExchangesController {

  private final PublicationService publicationService;
  private final TradeMapper tradeMapper;

  public ExchangesController(PublicationService publicationService, TradeMapper tradeMapper) {
    this.publicationService = publicationService;
    this.tradeMapper = tradeMapper;
  }

  /**
   * Crea una publicación de intercambio.
   */
  @PostMapping
  public ResponseEntity<Map<String, Object>> createPublication(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateTradePublicationDto dto
  ) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
    TradePublication publication = publicationService.createPublication(userId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Publicación creada con éxito",
        "publicationId", publication.getId()
    ));
  }

  /**
   * Búsqueda paginada de publicaciones activas.
   */
  @GetMapping
  public ResponseEntity<PaginationDtoOutput<TradePublicationDto>> searchActivePublications(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer per_page,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String country,
      @RequestParam(required = false) String team,
      @RequestParam(required = false) Category category,
      @RequestParam(required = false) CardType cardType
  ) {
    SearchPublicationsFilters filters = new SearchPublicationsFilters(name, country, team, category, cardType);
    Page<TradePublication> result = publicationService.searchActivePublications(page, per_page, filters);
    return ResponseEntity.ok(new PaginationDtoOutput<>(
        tradeMapper.mapPublications(result.getContent()),
        result.getNumber() + 1,
        result.getTotalPages()
    ));
  }

  /**
   * Publicaciones creadas por el usuario actual.
   */
  @GetMapping("/createdByMe")
  public ResponseEntity<PaginationDtoOutput<TradePublicationDto>> getMyPublications(
      @RequestAttribute("userId") String userId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer per_page
  ) {
    Page<TradePublication> result = publicationService.getMyPublications(userId, page, per_page);
    return ResponseEntity.ok(new PaginationDtoOutput<>(
        tradeMapper.mapPublications(result.getContent()),
        result.getNumber() + 1,
        result.getTotalPages()
    ));
  }

  /**
   * Cancela una publicación del usuario actual.
   */
  @DeleteMapping("/{publicationId}")
  public ResponseEntity<Void> cancelPublication(
      @PathVariable String publicationId,
      @RequestAttribute("userId") String userId
  ) throws UserNotFoundException, NotFoundException, ForbiddenException {
    publicationService.cancelPublication(userId, publicationId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Crea una propuesta sobre una publicación.
   */
  @PostMapping("/{publicationId}/proposals")
  public ResponseEntity<Map<String, Object>> createProposal(
      @PathVariable String publicationId,
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateTradeProposalDTO body
  ) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException, NoAvailableSlotsException {
    CreateTradeProposalDTO dto = new CreateTradeProposalDTO(publicationId, body.cardIds());
    publicationService.createTradeProposalForPublication(userId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Propuesta enviada con éxito"
    ));
  }

  /**
   * Acepta o rechaza una propuesta.
   */
  @PutMapping("/{publicationId}/proposals/{proposalId}")
  public ResponseEntity<Map<String, Object>> reviewProposal(
      @PathVariable String publicationId,
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody ReviewProposalDto body
  ) throws UserNotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException, MissingCardException, InsufficientCardException {
    body.setPublicationId(publicationId);
    body.setProposalId(proposalId);
    publicationService.reviewProposal(userId, body);
    return ResponseEntity.ok(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Propuesta procesada con éxito"
    ));
  }

  /**
   * Agrega feedback sobre una publicación finalizada.
   */
  @PostMapping("/feedback")
  public ResponseEntity<Map<String, Object>> addFeedback(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody NewFeedbackDto dto
  ) throws UserNotFoundException, NotFoundException {
    publicationService.addFeedback(userId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Feedback agregado con éxito"
    ));
  }
}
