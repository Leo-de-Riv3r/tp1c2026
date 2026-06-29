package com.tacs.tp1c2026.entities.dto.user.output;

import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;

/**
 * Vista de API de una figurita en la colección del user. Desacopla la respuesta del entity
 * embebido de Mongo: expone solo lo que el FE necesita ({@code available} = cantidad disponible
 * tras descontar las comprometidas) y oculta campos internos (compromisedCount, acquisitionDate,
 * acquisitionOrigin).
 */
public record CollectionCardDto(
    String cardId,
    Integer number,
    String description,
    String country,
    String team,
    String category,
    Integer quantity,
    Integer available
) {
    public static CollectionCardDto from(CollectionCard card) {
        return new CollectionCardDto(
            card.getCardId(),
            card.getNumber(),
            card.getDescription(),
            card.getCountry(),
            card.getTeam(),
            card.getCategory(),
            card.getQuantity(),
            card.getAvailable()
        );
    }
}
