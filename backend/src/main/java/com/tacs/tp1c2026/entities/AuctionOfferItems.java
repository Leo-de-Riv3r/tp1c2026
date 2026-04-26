package com.tacs.tp1c2026.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuctionOfferItems {
  private Figurita figurita;
  private Integer cantidad;

  public AuctionOfferItems(Figurita figurita, Integer cantidad){
    this.figurita = figurita;
    this.cantidad = cantidad;
  }
}
