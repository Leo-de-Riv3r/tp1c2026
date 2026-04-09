package com.tacs.tp1c2026.entities.ReglasStrategies;

import com.tacs.tp1c2026.entities.OfertaSubasta;

public class CantidadFiguritasStrategy implements IReglaStrategy {
  private Integer cantidadMinimaFiguritas;

  public CantidadFiguritasStrategy(Integer cantidadMinimaFiguritas) {
    this.cantidadMinimaFiguritas = cantidadMinimaFiguritas;
  }

  public Boolean cumpleRegla(OfertaSubasta oferta) {
    return oferta.getFiguritasOfrecidas().size() >= this.cantidadMinimaFiguritas;
  }
}
