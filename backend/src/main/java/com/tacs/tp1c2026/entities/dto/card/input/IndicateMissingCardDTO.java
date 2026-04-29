package com.tacs.tp1c2026.entities.dto.card.input;

/**
 * DTO used when a user indicates a missing sticker. Contains the identifier of the sticker.
 */
public record IndicateMissingCardDTO(
    Integer stickerId
) {}

