package com.tacs.tp1c2026.entities.dto.statistics.output;

/**
 * Subasta activa con más ofertas PENDING. {@code pendingOffers} es la métrica de orden;
 * {@code totalOffers} se incluye para que el FE muestre histórico vs activo.
 */
public record TopAuctionByOffersEntry(
    String auctionId,
    String cardId,
    String cardDescription,
    String publisherName,
    long pendingOffers,
    long totalOffers
) {}
