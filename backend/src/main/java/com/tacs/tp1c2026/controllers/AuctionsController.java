package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.dto.common.ApiResponse;
import com.tacs.tp1c2026.entities.dto.auction.input.CancelAuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;
import com.tacs.tp1c2026.entities.dto.auction.input.CreationAuctionOfferDTO;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionOfferDto;
import com.tacs.tp1c2026.entities.dto.auction.output.UserBidDto;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.mappers.AuctionMapper;
import com.tacs.tp1c2026.entities.enums.CardType;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.exceptions.AuctionClosedException;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import com.tacs.tp1c2026.exceptions.MissingCardException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.OfferAlreadyProcessedException;
import com.tacs.tp1c2026.exceptions.OfferNotFoundException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.services.AuctionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
  public ResponseEntity<ApiResponse<AuctionDto>> createAuction(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateAuctionDTO dto
  ) throws InsufficientCardException, MissingCardException, NotFoundException, UserNotFoundException {
    Auction auction = auctionService.createAuction(userId, dto);
    AuctionDto body = auctionMapper.mapAuction(auction);
    return ResponseEntity
        .created(URI.create("/api/auctions/" + auction.getId()))
        .body(ApiResponse.of("Subasta creada con éxito", body));
  }

  /**
   * Búsqueda paginada de subastas.
   *   - Si se envía {@code userId}, devuelve todas las subastas creadas por ese usuario
   *     (cualquier estado), sin aplicar los filtros de actividad.
   *   - Si no, devuelve subastas activas con filtros opcionales (name, country, team,
   *     category, cardType).
   */
  @GetMapping
  public ResponseEntity<PaginationDtoOutput<AuctionDto>> searchAuctions(
      @RequestAttribute("userId") String currentUserId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer per_page,
      @RequestParam(required = false) String userId,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String country,
      @RequestParam(required = false) String team,
      @RequestParam(required = false) Category category,
      @RequestParam(required = false) CardType cardType
  ) {
    if (userId != null) {
      Page<Auction> result = auctionService.getMyAuctions(userId, page, per_page);
      return ResponseEntity.ok(new PaginationDtoOutput<>(
          auctionMapper.mapAuctions(result.getContent()),
          result.getNumber() + 1,
          result.getTotalPages()
      ));
    }
    SearchPublicationsFilters filters = new SearchPublicationsFilters(name, country, team, category, cardType, null);
    return ResponseEntity.ok(auctionService.searchActiveAuctions(page, per_page, filters));
  }

  /**
   * Ofertas hechas por un usuario (vista "Mis Ofertas"). Devuelve una lista plana de
   * {@link UserBidDto}, no de Auction (shape distinto al search por eso es endpoint propio).
   * Si no se envía {@code userId}, usa el del token.
   */
  @GetMapping("/offers")
  public ResponseEntity<java.util.List<UserBidDto>> getOffers(
      @RequestAttribute("userId") String currentUserId,
      @RequestParam(required = false) String userId
  ) {
    String targetUserId = userId != null ? userId : currentUserId;
    return ResponseEntity.ok(auctionService.getMyOffers(targetUserId));
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
  public ResponseEntity<ApiResponse<AuctionOfferDto>> createOffer(
      @PathVariable String auctionId,
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreationAuctionOfferDTO body
  ) throws InsufficientCardException, MissingCardException, NotFoundException, UserNotFoundException {
    CreationAuctionOfferDTO dto = new CreationAuctionOfferDTO(auctionId, body.items());
    AuctionOffer offer = auctionService.createAuctionOffer(userId, dto);
    AuctionOfferDto offerDto = auctionMapper.mapOffer(auctionId, offer);
    return ResponseEntity
        .created(URI.create("/api/auctions/" + auctionId + "/offers/" + offer.getId()))
        .body(ApiResponse.of("Oferta publicada con éxito", offerDto));
  }

  /**
   * Marca al usuario como interesado en la subasta.
   */
  @PostMapping("/{auctionId}/interested")
  public ResponseEntity<ApiResponse<Void>> addInterested(
      @PathVariable String auctionId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, UserNotFoundException {
    auctionService.addInterestedUser(auctionId, userId);
    return ResponseEntity.ok(ApiResponse.of("Marcado como interesado"));
  }

  /**
   * Cancela una subasta del usuario.
   */
  @DeleteMapping("/{auctionId}")
  public ResponseEntity<ApiResponse<Void>> cancelAuction(
      @PathVariable String auctionId,
      @RequestAttribute("userId") String userId
  ) throws AuctionClosedException, NotFoundException, UserNotFoundException, ForbiddenException {
    CancelAuctionDto dto = new CancelAuctionDto();
    dto.setAuctionId(auctionId);
    auctionService.cancelAuction(userId, dto);
    return ResponseEntity.ok(ApiResponse.of("Subasta cancelada"));
  }

  /**
   * Cancela una oferta del usuario.
   */
  @DeleteMapping("/{auctionId}/offers/{offerId}")
  public ResponseEntity<ApiResponse<Void>> cancelAuctionOffer(
      @PathVariable String auctionId,
      @PathVariable String offerId,
      @RequestAttribute("userId") String userId
  ) {
    auctionService.cancelOffer(offerId, userId, auctionId);
    return ResponseEntity.ok(ApiResponse.of("Oferta cancelada"));
  }

  @PutMapping("/{auctionId}/offers/{offerId}/best")
  public ResponseEntity<ApiResponse<Void>> setOfferAsBest(
      @PathVariable String auctionId,
      @PathVariable String offerId,
      @RequestAttribute("userId") String userId
  ) {
    auctionService.setAuctionOfferAsBest(auctionId, offerId, userId);
    return ResponseEntity.ok(ApiResponse.of("Oferta marcada como mejor"));
  }

  @PutMapping("/{auctionId}/offers/{offerId}/reject")
  public ResponseEntity<ApiResponse<Void>> rejectOffer(
      @PathVariable String auctionId,
      @PathVariable String offerId,
      @RequestAttribute("userId") String userId
  ) {
    auctionService.rejectAuctionOffer(auctionId, offerId, userId);
    return ResponseEntity.ok(ApiResponse.of("Oferta rechazada"));
  }

  @PutMapping("/{auctionId}/offers/{offerId}/accept")
  public ResponseEntity<ApiResponse<Void>> acceptOffer(
      @PathVariable String auctionId,
      @PathVariable String offerId,
      @RequestAttribute("userId") String userId
  ) throws AuctionClosedException, NotFoundException, UserNotFoundException, ForbiddenException, OfferAlreadyProcessedException, OfferNotFoundException {
    auctionService.acceptAuctionOffer(auctionId, offerId, userId);
    return ResponseEntity.ok(ApiResponse.of("Oferta aceptada"));
  }

}
