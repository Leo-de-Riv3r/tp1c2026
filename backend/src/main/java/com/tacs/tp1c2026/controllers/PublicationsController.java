package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.common.ApiResponse;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradePublicationDto;
import com.tacs.tp1c2026.entities.dto.trade.output.TradePublicationDto;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.config.RequiresOwnerOrAdmin;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.services.PublicationService;
import com.tacs.tp1c2026.services.mappers.TradeMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
   * Creates a trade publication.
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TradePublicationDto>> createPublication(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateTradePublicationDto dto
  ) throws NotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
    TradePublication publication = publicationService.createPublication(userId, dto);
    TradePublicationDto body = tradeMapper.mapPublication(publication);
    return ResponseEntity
        .created(URI.create("/api/publications/" + publication.getId()))
        .body(ApiResponse.of("Publication created successfully", body));
  }

  /**
   * Paginated search of publications.
   * If {@code userId} is sent, returns that user's publications (all statuses).
   * Otherwise, returns active publications (with optional filters by name, country, team, category).
   */
  @GetMapping
  @RequiresOwnerOrAdmin(value = "userId", source = RequiresOwnerOrAdmin.Source.QUERY)
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
      // Búsqueda activa: excluye las publicaciones del propio user (no tiene sentido proponerse a uno mismo).
      SearchPublicationsFilters filters = new SearchPublicationsFilters(name, country, team, category, null, null, currentUserId);
      result = publicationService.searchActivePublications(page, per_page, filters);
    }
    return ResponseEntity.ok(new PaginationDtoOutput<>(
        tradeMapper.mapPublications(result.getContent()),
        result.getNumber() + 1,
        result.getTotalPages()
    ));
  }

  /**
   * Publication detail.
   */
  @GetMapping("/{publicationId}")
  public ResponseEntity<TradePublicationDto> getPublication(
      @PathVariable String publicationId
  ) throws NotFoundException {
    TradePublication publication = publicationService.findPublication(publicationId);
    return ResponseEntity.ok(tradeMapper.mapPublication(publication));
  }

  /**
   * Cancels a publication from the current user.
   */
  @DeleteMapping("/{publicationId}")
  public ResponseEntity<ApiResponse<Void>> cancelPublication(
      @PathVariable String publicationId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, NotFoundException, ForbiddenException {
    publicationService.cancelPublication(userId, publicationId);
    return ResponseEntity.ok(ApiResponse.of("Publication cancelled"));
  }
}
