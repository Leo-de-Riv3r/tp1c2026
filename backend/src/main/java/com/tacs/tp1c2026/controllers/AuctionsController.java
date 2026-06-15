package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresOwnerOrAdmin;
import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.dto.common.ApiResponse;
import com.tacs.tp1c2026.entities.dto.auction.input.CancelAuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreationAuctionOfferDto;
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
   * Creates a new auction on a duplicate card from the user's collection.
   */
  @PostMapping
  public ResponseEntity<ApiResponse<AuctionDto>> createAuction(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateAuctionDto dto
  ) throws InsufficientCardException, MissingCardException, NotFoundException, NotFoundException {
    Auction auction = auctionService.createAuction(userId, dto);
    AuctionDto body = auctionMapper.mapAuction(auction);
    return ResponseEntity
        .created(URI.create("/api/auctions/" + auction.getId()))
        .body(ApiResponse.of("Auction created successfully", body));
  }

  /**
   * Paginated search of auctions.
   *   - If {@code userId} is sent, returns all auctions created by that user
   *     (any status), without applying activity filters.
   *   - Otherwise, returns active auctions with optional filters (name, country, team,
   *     category, cardType).
   */
  @GetMapping
  @RequiresOwnerOrAdmin(value = "userId", source = RequiresOwnerOrAdmin.Source.QUERY)
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
    // Búsqueda activa: excluye las subastas del propio user (no tiene sentido ofertar en una propia).
    SearchPublicationsFilters filters = new SearchPublicationsFilters(name, country, team, category, cardType, null, currentUserId);
    return ResponseEntity.ok(auctionService.searchActiveAuctions(page, per_page, filters));
  }

  /**
   * Offers made by a user ("My Offers" view). Returns a flat list of {@link UserBidDto},
   * not Auction (different shape from search, hence its own endpoint).
   * If {@code userId} is not sent, uses the token user.
   */
  @GetMapping("/offers")
  @RequiresOwnerOrAdmin(value = "userId", source = RequiresOwnerOrAdmin.Source.QUERY)
  public ResponseEntity<java.util.List<UserBidDto>> getOffers(
      @RequestAttribute("userId") String currentUserId,
      @RequestParam(required = false) String userId
  ) {
    String targetUserId = userId != null ? userId : currentUserId;
    return ResponseEntity.ok(auctionService.getMyOffers(targetUserId));
  }

  /**
   * Auction detail.
   */
  @GetMapping("/{auctionId}")
  public ResponseEntity<AuctionDto> getAuction(@PathVariable String auctionId) throws NotFoundException {
    Auction auction = auctionService.getAuctionById(auctionId);
    return ResponseEntity.ok(auctionMapper.mapAuction(auction));
  }

  /**
   * Creates an offer on an existing auction.
   */
  @PostMapping("/{auctionId}/offers")
  public ResponseEntity<ApiResponse<AuctionOfferDto>> createOffer(
      @PathVariable String auctionId,
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreationAuctionOfferDto body
  ) throws InsufficientCardException, MissingCardException, NotFoundException, NotFoundException {
    AuctionOffer offer = auctionService.createAuctionOffer(userId, auctionId, body);
    AuctionOfferDto offerDto = auctionMapper.mapOffer(auctionId, offer);
    return ResponseEntity
        .created(URI.create("/api/auctions/" + auctionId + "/offers/" + offer.getId()))
        .body(ApiResponse.of("Offer published successfully", offerDto));
  }

  /**
   * Marks the user as interested in the auction.
   */
  @PostMapping("/{auctionId}/interested")
  public ResponseEntity<ApiResponse<Void>> addInterested(
      @PathVariable String auctionId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, NotFoundException {
    auctionService.addInterestedUser(auctionId, userId);
    return ResponseEntity.ok(ApiResponse.of("Marked as interested"));
  }

  /**
   * Cancels an auction from the user.
   */
  @DeleteMapping("/{auctionId}")
  public ResponseEntity<ApiResponse<Void>> cancelAuction(
      @PathVariable String auctionId,
      @RequestAttribute("userId") String userId
  ) throws AuctionClosedException, NotFoundException, NotFoundException, ForbiddenException {
    CancelAuctionDto dto = new CancelAuctionDto();
    dto.setAuctionId(auctionId);
    auctionService.cancelAuction(userId, dto);
    return ResponseEntity.ok(ApiResponse.of("Auction cancelled"));
  }

  /**
   * Cancels an offer from the user.
   */
  @DeleteMapping("/{auctionId}/offers/{offerId}")
  public ResponseEntity<ApiResponse<Void>> cancelAuctionOffer(
      @PathVariable String auctionId,
      @PathVariable String offerId,
      @RequestAttribute("userId") String userId
  ) {
    auctionService.cancelOffer(offerId, userId, auctionId);
    return ResponseEntity.ok(ApiResponse.of("Offer cancelled"));
  }

  @PutMapping("/{auctionId}/offers/{offerId}/best")
  public ResponseEntity<ApiResponse<Void>> setOfferAsBest(
      @PathVariable String auctionId,
      @PathVariable String offerId,
      @RequestAttribute("userId") String userId
  ) {
    auctionService.setAuctionOfferAsBest(auctionId, offerId, userId);
    return ResponseEntity.ok(ApiResponse.of("Offer marked as best"));
  }

  @PutMapping("/{auctionId}/offers/{offerId}/reject")
  public ResponseEntity<ApiResponse<Void>> rejectOffer(
      @PathVariable String auctionId,
      @PathVariable String offerId,
      @RequestAttribute("userId") String userId
  ) {
    auctionService.rejectAuctionOffer(auctionId, offerId, userId);
    return ResponseEntity.ok(ApiResponse.of("Offer rejected"));
  }

  @PutMapping("/{auctionId}/offers/{offerId}/accept")
  public ResponseEntity<ApiResponse<Void>> acceptOffer(
      @PathVariable String auctionId,
      @PathVariable String offerId,
      @RequestAttribute("userId") String userId
  ) throws AuctionClosedException, NotFoundException, NotFoundException, ForbiddenException, OfferAlreadyProcessedException, OfferNotFoundException {
    auctionService.acceptAuctionOffer(auctionId, offerId, userId);
    return ResponseEntity.ok(ApiResponse.of("Offer accepted"));
  }

}
