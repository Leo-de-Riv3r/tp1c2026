package com.tacs.tp1c2026.entities.dto.user.output;

import com.tacs.tp1c2026.entities.user.embedded.MissingCard;

import java.time.LocalDate;

/**
 * Vista de API de una figurita faltante del user. Desacopla la respuesta del entity embebido de Mongo.
 */
public record MissingCardDto(
    String cardId,
    Integer number,
    String description,
    String country,
    String team,
    String category,
    LocalDate addedAt
) {
    public static MissingCardDto from(MissingCard card) {
        return new MissingCardDto(
            card.getCardId(),
            card.getNumber(),
            card.getDescription(),
            card.getCountry(),
            card.getTeam(),
            card.getCategory(),
            card.getAddedAt()
        );
    }
}
