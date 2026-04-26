package com.tacs.tp1c2026.entities.user.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import com.tacs.tp1c2026.entities.card.Card;

// subdoc que vive dentro del usuario en mongo, representa a una figurita de su colección
// nuevo: compromisedCount (figuritas que ya ofreció o propuso - es decir, activas) y availableCount que es la cantidad - la comprometida

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionCard {

    private String cardId;
    private Integer number;
    private String description; // nombre del jugador, del equipo, de la copa, etc
    private String country; // puede ser null
    private String team; // puede ser null
    private String category; // "COMUN" | "EPICO" | "LEGENDARIO", no como enum porque esto no es la figurita del catalogo
    private Integer quantity;
    @Builder.Default
    private Integer compromisedCount = 0;
    private LocalDate adquisitionDate;
    private String adquisitionOrigin; // MANUAL | PROPUESTA | SUBASTA

    // Crea un subdocumento para collection (del usuario) a partir de una figurita del catálogo.
    // Quantity inicial: 1. Origen: MANUAL (cargado por el usuario)
    public static CollectionCard fromCatalog(Card figurita) {
        return CollectionCard.builder()
            .cardId(figurita.getId())
            .number(figurita.getNumber())
            .description(figurita.getDescription())
            .country(figurita.getCountry())
            .team(figurita.getTeam())
            .category(figurita.getCategory())
            .quantity(1)
            .compromisedCount(0)
            .adquisitionDate(LocalDate.now())
            .adquisitionOrigin("MANUAL")
            .build();
    }
}