package com.tacs.tp1c2026.entities.dto.trade.output;

import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;

public record TradePublicationDto(
    String publicationId,
    Integer initialCount,
    Integer remainingCount,
    Integer cardNumber,
    PublicationStatus status,
    String cardDescription,
    String cardCountry,
    String cardTeam,
    Category cardCategory
) {}
