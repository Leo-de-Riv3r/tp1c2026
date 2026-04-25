/*
    Representa al documento de la figurita de catálogo, que es la base de las que se muestran en todos los demás docs
    El catálogo va a estar cargado en la base y va a ser inmutable    
*/

package com.tacs.tp1c2026.entities;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "stickers")
public class Sticker {
    @Id
    private Integer id;
    @Getter
    private Integer number;
    private String description;
    private String country;
    private String team;
    private String category;
}
