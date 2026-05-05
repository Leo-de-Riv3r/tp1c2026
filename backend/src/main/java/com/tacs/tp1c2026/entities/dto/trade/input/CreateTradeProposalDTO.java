package com.tacs.tp1c2026.entities.dto.trade.input;

import java.util.List;

/**
 * DTO para crear una propuesta sobre una publicación.
 * - {@code cardIds}: lista de cards ofrecidas (con repetidos según cantidad).
 * - {@code requestedCount}: cantidad de unidades del card publicado que pide el bidder.
 *   Tiene que cumplir 1 ≤ requestedCount ≤ publication.remainingCount.
 */
public record CreateTradeProposalDTO(
    String publicationId,
    List<String> cardIds,
    Integer requestedCount
) {}
