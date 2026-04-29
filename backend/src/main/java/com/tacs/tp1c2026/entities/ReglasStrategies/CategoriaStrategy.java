/*
package com.tacs.tp1c2026.entities.ReglasStrategies;

import com.tacs.tp1c2026.entities.AuctionOffer;
import com.tacs.tp1c2026.entities.enums.CardCategory;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@DiscriminatorValue("CATEGORIA")
public class CategoriaStrategy extends IReglaStrategy {

  @Column
  private Integer cantidadMinima;

  @Enumerated(EnumType.STRING)
  @Column
  private CardCategory categoriaRequerida;

  protected CategoriaStrategy() {}

  public CategoriaStrategy(Integer cantidadMinima, CardCategory categoriaRequerida) {
    this.cantidadMinima = cantidadMinima;
    this.categoriaRequerida = categoriaRequerida;
  }

  @Override
  public Boolean cumpleRegla(AuctionOffer ofertaSubasta) {
    Integer cantidadCategoria = ofertaSubasta.getItemsOfrecidos().stream()
        .filter(item -> item.getCard() != null)
        .filter(item -> item.getCard().getCategoria() == categoriaRequerida)
        .map(item -> item.getCantidad() == null ? 0 : item.getCantidad())
        .reduce(0, Integer::sum);
    return cantidadCategoria > cantidadMinima;
  }
}
*/
