package com.tacs.tp1c2026.entities.dto.mappers;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.card.output.CardDTO;


public class CardMapper {

    public static CardDTO toDto(Card s) {
        return new CardDTO(
                s.getNumber(),
                s.getPlayer(),
                s.getCountry(),
                s.getTeam(),
                s.getDescription(),
                s.getCategory()
        );
    }
}

