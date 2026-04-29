/*
package com.tacs.tp1c2026.entities.ReglasStrategies;

import com.tacs.tp1c2026.entities.AuctionOffer;
import com.tacs.tp1c2026.entities.Auction;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "regla_strategy")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_regla", discriminatorType = DiscriminatorType.STRING)
public abstract class IReglaStrategy {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "subasta_id")
  private Auction subasta;

  protected IReglaStrategy() {}

  public Integer getId() {
    return id;
  }

  public Auction getAuction() {
    return subasta;
  }

  public void setAuction(Auction subasta) {
    this.subasta = subasta;
  }

  public abstract Boolean cumpleRegla(AuctionOffer ofertaSubasta);
}
*/
