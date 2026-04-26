package com.tacs.tp1c2026.entities.dto.input;

import java.util.List;

/**
 * DTO for creating an auction offer. Contains auctionId and a list of offered items with quantities.
 */
public record CreationAuctionOfferDTO(
    Integer auctionId,
    List<Item> items
) {
    public static record Item(Integer stickerId, Integer amount) {}
}

