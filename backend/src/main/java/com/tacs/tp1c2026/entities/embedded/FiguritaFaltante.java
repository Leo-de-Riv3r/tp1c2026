package com.tacs.tp1c2026.entities.embedded;

import com.tacs.tp1c2026.entities.Figurita;
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
public class FiguritaFaltante {
    private String figuritaId;
    private Integer number;
    private String description;
    private String country;
    private String team;
    private String category;

    // Crea un subdocumento de faltante a partir de una figurita del catálogo
    public static FiguritaFaltante fromCatalog(Figurita figurita) {
        return FiguritaFaltante.builder()
            .figuritaId(figurita.getId())
            .number(figurita.getNumber())
            .description(figurita.getDescription())
            .country(figurita.getCountry())
            .team(figurita.getTeam())
            .category(figurita.getCategory())
            .build();
    }
}