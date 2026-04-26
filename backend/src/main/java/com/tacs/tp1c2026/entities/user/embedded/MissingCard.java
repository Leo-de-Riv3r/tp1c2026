package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.card.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Subdocumento que vive dentro del array missingCards del usuario en mongo, representa a una figurita que le falta en la collection
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissingCard {
    private String cardId;
    private Integer number;
    private String description;
    private String country;
    private String team;
    private String category;

    // Crea un subdocumento de faltante a partir de una figurita del catálogo
    public static MissingCard fromCatalog(Card card) {
        return MissingCard.builder()
            .cardId(card.getId())
            .number(card.getNumber())
            .description(card.getDescription())
            .country(card.getCountry())
            .team(card.getTeam())
            .category(card.getCategory())
            .build();
    }
}