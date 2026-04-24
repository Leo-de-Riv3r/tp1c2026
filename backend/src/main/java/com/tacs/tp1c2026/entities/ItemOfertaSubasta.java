package com.tacs.tp1c2026.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "items_oferta_subasta")
@TypeAlias("itemOfertaSubasta")
@Getter
@Setter
@NoArgsConstructor
public class ItemOfertaSubasta {

  @Id
  private Integer id;

  private Figurita figurita;

  private Integer cantidad;

  public ItemOfertaSubasta(Figurita figurita, Integer cantidad){
    this.figurita = figurita;
    this.cantidad = cantidad;
  }

}
