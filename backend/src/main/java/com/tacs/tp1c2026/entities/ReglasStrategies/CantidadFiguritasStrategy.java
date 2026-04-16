package com.tacs.tp1c2026.entities.ReglasStrategies;

import com.tacs.tp1c2026.entities.OfertaSubasta;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CANTIDAD")
public class CantidadFiguritasStrategy extends IReglaStrategy {

  @Column
  private Integer cantidadMinimaFiguritas;

  protected CantidadFiguritasStrategy() {}

  public CantidadFiguritasStrategy(Integer cantidadMinimaFiguritas) {
    this.cantidadMinimaFiguritas = cantidadMinimaFiguritas;
  }

  /**
   * Verifica que la oferta contenga al menos {@code cantidadMinimaFiguritas} figuritas ofrecidas.
   *
   * @param oferta oferta de subasta a evaluar
   * @return {@code true} si la cantidad de figuritas ofrecidas es mayor o igual al mínimo requerido
   */
  @Override
  public Boolean cumpleRegla(OfertaSubasta oferta) {
    return oferta.getTotalFiguritas() >= this.cantidadMinimaFiguritas;
  }
}
