/*
    Representa al documento de la figurita de catálogo, que es la base de las que se muestran en todos los demás docs
    El catálogo va a estar cargado en la base y va a ser inmutable    
*/

package com.tacs.tp1c2026.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "figuritas")
public class Figurita {
    @Id
    private String id;
    private Integer number;
    private String description;
    private String country;
    private String team;
    private String category;
}
