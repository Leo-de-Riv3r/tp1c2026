package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.CardCategory;
import com.tacs.tp1c2026.entities.enums.PublicationState;

public record ExchangePublicationDto(
    String publicationId,
    Integer quantity,
    Integer cardNumber,
    PublicationState state,
    String name,
    String description,
    String country,
    String team,
    CardCategory category
) {}

