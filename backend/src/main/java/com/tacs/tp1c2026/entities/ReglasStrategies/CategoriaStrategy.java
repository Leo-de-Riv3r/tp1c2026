package com.tacs.tp1c2026.entities.ReglasStrategies;

import com.tacs.tp1c2026.entities.OfertaSubasta;
import com.tacs.tp1c2026.entities.enums.Categoria;

public class CategoriaStrategy implements IReglaStrategy{
  private Integer cantidadMinima;
  private Categoria categoriaRequerida;

  @Override
  public Boolean cumpleRegla(OfertaSubasta ofertaSubasta) {
    return ofertaSubasta.getFiguritasOfrecidas().stream()
        .filter(figurita -> figurita.getCategoria() == categoriaRequerida)
        .toList()
        .size() > cantidadMinima;
  }
}
