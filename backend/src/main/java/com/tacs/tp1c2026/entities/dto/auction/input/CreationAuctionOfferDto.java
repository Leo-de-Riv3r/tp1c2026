package com.tacs.tp1c2026.entities.dto.auction.input;

import java.util.List;

/**
 * DTO para crear una oferta de subasta. Contiene el auctionId y una lista de ítems ofrecidos con sus cantidades.
 */
public record CreationAuctionOfferDto(
    List<Item> items
) {
    public static record Item(String cardId, Integer amount) {}
}
