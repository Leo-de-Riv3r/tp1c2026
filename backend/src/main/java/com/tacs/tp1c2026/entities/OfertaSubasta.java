package com.tacs.tp1c2026.entities;

import java.util.List;
import lombok.Getter;

@Getter
public class OfertaSubasta {
  private Usuario postor;
  private List<Figurita> figuritasOfrecidas;

  public OfertaSubasta(Usuario postor, List<Figurita> figuritasOfrecidas) {
    this.postor = postor;
    this.figuritasOfrecidas = figuritasOfrecidas;
  }
}
