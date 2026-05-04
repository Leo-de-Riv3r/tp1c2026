package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.dto.auction.input.CancelAuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;
import com.tacs.tp1c2026.entities.dto.auction.input.CreationAuctionOfferDTO;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.exceptions.AuctionClosedException;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import com.tacs.tp1c2026.exceptions.MissingCardException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.services.AuctionService;
import com.tacs.tp1c2026.services.mappers.AuctionMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auctions")
public class AuctionsController {

  private final AuctionService auctionService;
  private final AuctionMapper auctionMapper;

  public AuctionsController(AuctionService auctionService, AuctionMapper auctionMapper) {
    this.auctionService = auctionService;
    this.auctionMapper = auctionMapper;
  }

  /**
   * Crea una nueva subasta sobre una figurita repetida del usuario.
   */
  @PostMapping
  public ResponseEntity<Map<String, Object>> createAuction(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateAuctionDTO dto
  ) throws InsufficientCardException, MissingCardException, NotFoundException, UserNotFoundException {
    Auction auction = auctionService.createAuction(userId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Subasta creada con éxito",
        "auctionId", auction.getId()
    ));
  }

  /**
   * Búsqueda paginada de subastas activas con filtros.
   */
  @GetMapping
  public ResponseEntity<PaginationDtoOutput<AuctionDto>> searchActiveAuctions(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer per_page,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String country,
      @RequestParam(required = false) String team,
      @RequestParam(required = false) Category category
  ) {
    SearchPublicationsFilters filters = new SearchPublicationsFilters(name, country, team, category);
    Page<Auction> result = auctionService.searchActiveAuctions(page, per_page, filters);
    return ResponseEntity.ok(new PaginationDtoOutput<>(
        auctionMapper.mapAuctions(result.getContent()),
        result.getNumber() + 1,
        result.getTotalPages()
    ));
  }

  /**
   * Subastas creadas por el usuario actual.
   */
  @GetMapping("/createdByMe")
  public ResponseEntity<PaginationDtoOutput<AuctionDto>> getMyAuctions(
      @RequestAttribute("userId") String userId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer per_page
  ) {
    Page<Auction> result = auctionService.getMyAuctions(userId, page, per_page);
    return ResponseEntity.ok(new PaginationDtoOutput<>(
        auctionMapper.mapAuctions(result.getContent()),
        result.getNumber() + 1,
        result.getTotalPages()
    ));
  }

  /**
   * Detalle de una subasta.
   */
  @GetMapping("/{auctionId}")
  public ResponseEntity<AuctionDto> getAuction(@PathVariable String auctionId) throws NotFoundException {
    Auction auction = auctionService.getAuctionById(auctionId);
    return ResponseEntity.ok(auctionMapper.mapAuction(auction));
  }

  /**
   * Crea una oferta sobre una subasta existente.
   */
  @PostMapping("/{auctionId}/offers")
  public ResponseEntity<Map<String, Object>> createOffer(
      @PathVariable String auctionId,
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreationAuctionOfferDTO body
  ) throws InsufficientCardException, MissingCardException, NotFoundException, UserNotFoundException {
    CreationAuctionOfferDTO dto = new CreationAuctionOfferDTO(auctionId, body.items());
    auctionService.createAuctionOffer(userId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Oferta publicada con éxito"
    ));
  }

  /**
   * Marca al usuario como interesado en la subasta.
   */
  @PostMapping("/{auctionId}/interested")
  public ResponseEntity<Void> addInterested(
      @PathVariable String auctionId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, UserNotFoundException {
    auctionService.addInterestedUser(auctionId, userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Cancela una subasta del usuario.
   */
  @DeleteMapping("/{auctionId}")
  public ResponseEntity<Void> cancelAuction(
      @PathVariable String auctionId,
      @RequestAttribute("userId") String userId
  ) throws AuctionClosedException, NotFoundException, UserNotFoundException, ForbiddenException {
    CancelAuctionDto dto = new CancelAuctionDto();
    dto.setAuctionId(auctionId);
    auctionService.cancelAuction(userId, dto);
    return ResponseEntity.noContent().build();
  }
}
