package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.card.Card;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CardRepository extends MongoRepository<Card, String> {

    /**
     * When searchAvailable is implemented in CardService, add query methods here.
     * Spring Data generates the query from the method name, following findBy + FieldName + Condition.
     * Examples:
     *   List<Card> findByNumber(Integer number);
     *   List<Card> findByDescriptionContainingIgnoreCase(String description);
     *   List<Card> findByCountryIgnoreCase(String country);
     *   List<Card> findByCountryIgnoreCaseAndCategoryIgnoreCase(String country, String category);
     */
}
