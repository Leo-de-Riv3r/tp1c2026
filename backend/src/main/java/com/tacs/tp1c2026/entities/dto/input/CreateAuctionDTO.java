package com.tacs.tp1c2026.entities.dto.input;

import java.util.List;

/**
 * DTO for creating an auction.
 */
public record CreateAuctionDTO(
    Integer stickerId,
    Integer auctionDurationHours,
    List<AuctionConditionDTO> conditions
) {}




