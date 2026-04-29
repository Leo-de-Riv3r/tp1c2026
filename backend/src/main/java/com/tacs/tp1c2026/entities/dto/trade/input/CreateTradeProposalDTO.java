package com.tacs.tp1c2026.entities.dto.trade.input;

import java.util.List;

/**
 * DTO for creating a trade proposal tied to a publication.
 * Contains the list of sticker ids offered in the proposal.
 */
public record CreateTradeProposalDTO(
    List<Integer> stickerIds
) {}

