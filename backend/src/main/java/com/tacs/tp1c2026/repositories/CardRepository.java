package com.tacs.tp1c2026.repositories;


import com.tacs.tp1c2026.entities.card.Card;

import java.util.Optional;

public interface CardRepository extends Repository<Card, String> {

    Optional<Card> findByNumber(Integer number);
}
