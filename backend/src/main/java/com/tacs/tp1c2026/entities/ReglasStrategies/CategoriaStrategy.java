package com.tacs.tp1c2026.entities.ReglasStrategies;

import com.tacs.tp1c2026.entities.OfertaSubasta;
import com.tacs.tp1c2026.entities.enums.Categoria;
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
  private Categoria categoriaRequerida;

  protected CategoriaStrategy() {}

  public CategoriaStrategy(Integer cantidadMinima, Categoria categoriaRequerida) {
    this.cantidadMinima = cantidadMinima;
    this.categoriaRequerida = categoriaRequerida;
  }

  /**
   * Verifica que la oferta tenga más de {@code cantidadMinima} figuritas pertenecientes
   * a la categoría requerida.
   *
   * @param ofertaSubasta oferta de subasta a evaluar
   * @return {@code true} si la cantidad de figuritas de la categoría requerida supera el mínimo
   */
  @Override
  public Boolean cumpleRegla(OfertaSubasta ofertaSubasta) {
    Integer cantidadCategoria = ofertaSubasta.getItemsOfrecidos().stream()
        .filter(item -> item.getFigurita() != null)
        .filter(item -> item.getFigurita().getCategoria() == categoriaRequerida)
        .map(item -> item.getCantidad() == null ? 0 : item.getCantidad())
        .reduce(0, Integer::sum);
    return cantidadCategoria > cantidadMinima;
  }
}
