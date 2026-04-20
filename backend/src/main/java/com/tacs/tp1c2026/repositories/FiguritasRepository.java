package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Figurita;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import java.util.List;

public interface FiguritasRepository extends MongoRepository<Figurita, String> {
    /*
    Cuando se implemente searchAvailable en FiguritasService, agregar acá los métodos necesarios.
    la query se genera automáticamente a partir del nombre del método, siguiendo el patrón:
    findBy + NombreCampo + Condición
    x ej para searchAvailable:
        List<Figurita> findByNumber(Integer number);
        List<Figurita> findByDescriptionContainingIgnoreCase(String description);
        List<Figurita> findByCountryIgnoreCase(String country);
        List<Figurita> findByCountryIgnoreCaseAndCategoryIgnoreCase(String country, String category);
        List<Figurita> findByCountryIgnoreCaseAndCategoryIgnoreCaseAndTypeIgnoreCase(String country, String category, String type);
    */
}