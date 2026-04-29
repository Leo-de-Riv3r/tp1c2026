package com.tacs.tp1c2026.entities.dto.card.input;

import com.tacs.tp1c2026.entities.enums.PublicationType;

/**
 * DTO used to publish a sticker either as a trade publication or an auction.
 */
public record PublishCardDTO(
    Integer stickerId
) {}

