package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.ModoIntercambio;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "figuritas_coleccion")
public class Publicacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "figurita_id")
  private Figurita figurita;
  private Integer cantidad;

  @Enumerated
  @Column(name = "modo_intercambio")
  private ModoIntercambio modoIntercambio;

  public Publicacion() {
  }

  public Publicacion(Figurita figurita, Integer cantidad, ModoIntercambio modoIntercambio) {
    this.figurita = figurita;
    this.cantidad = cantidad;
    this.modoIntercambio = modoIntercambio;
  }

}

