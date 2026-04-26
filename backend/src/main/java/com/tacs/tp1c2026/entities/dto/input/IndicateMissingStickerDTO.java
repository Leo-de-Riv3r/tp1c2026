package com.tacs.tp1c2026.entities.dto.input;

/**
 * DTO used when a user indicates a missing sticker. Contains the identifier of the sticker.
 */
public record IndicateMissingStickerDTO(
    Integer stickerId
) {}

