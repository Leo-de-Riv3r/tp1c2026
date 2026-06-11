package com.tacs.tp1c2026.entities.dto.auction.input;

import java.util.List;

/**
 * DTO for creating an auction offer. Contains auctionId and a list of offered items with quantities.
 */
public record CreationAuctionOfferDto(
    List<Item> items
) {
    public static record Item(String cardId, Integer amount) {}
}
