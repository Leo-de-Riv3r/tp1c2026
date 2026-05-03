package com.tacs.tp1c2026.entities.dto.auction.input;

import java.util.List;

/**
 * DTO for creating an auction.
 */
public record CreateAuctionDTO(
    String cardId,
    Integer auctionDurationHours,
    List<AuctionConditionDTO> conditions
) {}




