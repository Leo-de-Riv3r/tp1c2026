package com.tacs.tp1c2026.entities.exchange.embedded;

import com.tacs.tp1c2026.entities.card.Card;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("card_snapshot")
public class CardSnapshot {

    private String cardId;
    private Integer number;
    private String description;
    private String country;
    private String team;
    private String category;

    public static CardSnapshot from(Card card) {
        return new CardSnapshot(
            card.getId(),
            card.getNumber(),
            card.getDescription(),
            card.getCountry(),
            card.getTeam(),
            card.getCategory() == null ? null : card.getCategory().getValue()
        );
    }
}
