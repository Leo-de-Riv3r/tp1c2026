package com.tacs.tp1c2026.entities.dto.input;

/**
 * DTO for creating a trade publication.
 * Contains the sticker id and the amount available to trade.
 */
public record CreateTradePublicationDto(
    Integer stickerId,
    Integer amount
) {}

