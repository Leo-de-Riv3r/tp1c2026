/*
    Representa al documento de la card de catálogo, que es la base de las que se muestran en todos los demás docs
    El catálogo va a estar cargado en la base y va a ser inmutable    
*/

package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.CardCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "figuritas")
@TypeAlias("figurita")
public class Card {
    @Id
    private String id;
    private Integer number;
    private String name;
    private String description;
    private String country;
    private String team;
    private CardCategory category;
}
