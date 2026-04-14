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

  @Override
  public Boolean cumpleRegla(OfertaSubasta ofertaSubasta) {
    return ofertaSubasta.getFiguritasOfrecidas().stream()
        .filter(figurita -> figurita.getCategoria() == categoriaRequerida)
        .toList()
        .size() > cantidadMinima;
  }
}
