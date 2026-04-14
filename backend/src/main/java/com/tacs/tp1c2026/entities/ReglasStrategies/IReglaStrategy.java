package com.tacs.tp1c2026.entities.ReglasStrategies;

import com.tacs.tp1c2026.entities.OfertaSubasta;
import com.tacs.tp1c2026.entities.Subasta;
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
  private Subasta subasta;

  protected IReglaStrategy() {}

  public Integer getId() {
    return id;
  }

  public Subasta getSubasta() {
    return subasta;
  }

  public void setSubasta(Subasta subasta) {
    this.subasta = subasta;
  }

  /**
   * Verifica si una oferta de subasta cumple con la regla definida por la implementación concreta.
   *
   * @param ofertaSubasta oferta de subasta a evaluar
   * @return {@code true} si la oferta cumple la regla; {@code false} en caso contrario
   */
  public abstract Boolean cumpleRegla(OfertaSubasta ofertaSubasta);
}
