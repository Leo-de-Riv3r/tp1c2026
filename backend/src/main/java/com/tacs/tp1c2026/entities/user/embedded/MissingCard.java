package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.card.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
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
    private LocalDate addedAt;

    public static MissingCard fromCatalog(Card card) {
        return MissingCard.builder()
            .cardId(card.getId())
            .number(card.getNumber())
            .description(card.getDescription())
            .country(card.getCountry())
            .team(card.getTeam())
            .category(card.getCategory() == null ? null : card.getCategory().getValue())
            .addedAt(LocalDate.now())
            .build();
    }

    public boolean isOf(String cardId) {
        return this.cardId != null && this.cardId.equals(cardId);
    }
}
