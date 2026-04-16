package com.tacs.tp1c2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class ItemOfertaSubasta {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "oferta_subasta_id", referencedColumnName = "id")
  private OfertaSubasta ofertaSubasta;

  @ManyToOne
  @JoinColumn(name = "figurita_id", referencedColumnName = "id")
  private Figurita figurita;

  @Column(nullable = false)
  private Integer cantidad;
}

