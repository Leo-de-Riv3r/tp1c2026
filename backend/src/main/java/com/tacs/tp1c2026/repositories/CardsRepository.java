package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Card;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CardsRepository extends MongoRepository<Card, String> {
    /*
    Cuando se implemente searchAvailable en CardsService, agregar acá los métodos necesarios.
    la query se genera automáticamente a partir del nombre del método, siguiendo el patrón:
    findBy + NombreCampo + Condición
    x ej para searchAvailable:
        List<Card> findByNumber(Integer number);
        List<Card> findByDescriptionContainingIgnoreCase(String description);
        List<Card> findByCountryIgnoreCase(String country);
        List<Card> findByCountryIgnoreCaseAndCategoryIgnoreCase(String country, String category);
        List<Card> findByCountryIgnoreCaseAndCategoryIgnoreCaseAndTypeIgnoreCase(String country, String category, String type);
    */
    Optional<Card> findByNumber(Integer number);
}