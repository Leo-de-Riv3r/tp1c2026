package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.common.ApiResponse;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradePublicationDto;
import com.tacs.tp1c2026.entities.dto.trade.output.TradePublicationDto;
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

@RestController
@RequestMapping("/publications")
public class PublicationsController {

  private final PublicationService publicationService;
  private final TradeMapper tradeMapper;

  public PublicationsController(PublicationService publicationService, TradeMapper tradeMapper) {
    this.publicationService = publicationService;
    this.tradeMapper = tradeMapper;
  }

  /**
   * Crea una publicación de intercambio.
   */
  @PostMapping
  public ResponseEntity<ApiResponse> createPublication(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateTradePublicationDto dto
  ) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
    TradePublication publication = publicationService.createPublication(userId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("Publicación creada con éxito", publication.getId()));
  }

  /**
   * Búsqueda paginada de publicaciones.
   * Si se envía {@code userId}, devuelve las publicaciones del usuario (todos los estados).
   * Si no, devuelve publicaciones activas (con filtros opcionales por nombre, país, equipo, categoría).
   */
  @GetMapping
  public ResponseEntity<PaginationDtoOutput<TradePublicationDto>> searchPublications(
      @RequestAttribute("userId") String currentUserId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer per_page,
      @RequestParam(required = false) String userId,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String country,
      @RequestParam(required = false) String team,
      @RequestParam(required = false) Category category
  ) {
    Page<TradePublication> result;
    if (userId != null) {
      result = publicationService.getMyPublications(userId, page, per_page);
    } else {
      SearchPublicationsFilters filters = new SearchPublicationsFilters(name, country, team, category, null, null);
      result = publicationService.searchActivePublications(page, per_page, filters);
    }
    return ResponseEntity.ok(new PaginationDtoOutput<>(
        tradeMapper.mapPublications(result.getContent()),
        result.getNumber() + 1,
        result.getTotalPages()
    ));
  }

  /**
   * Detalle de una publicación.
   */
  @GetMapping("/{publicationId}")
  public ResponseEntity<TradePublicationDto> getPublication(
      @PathVariable String publicationId
  ) throws NotFoundException {
    TradePublication publication = publicationService.findPublication(publicationId);
    return ResponseEntity.ok(tradeMapper.mapPublication(publication));
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
}
