package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table
@NoArgsConstructor
public class FiguritaColeccion {
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  @ManyToOne
  @JoinColumn(name = "figurita_id", referencedColumnName = "id")
  private Figurita figurita;
  @Column
  private Integer cantidad;
  @Enumerated(EnumType.STRING)
  @Column
  private TipoParticipacion tipoParticipacion;

  public FiguritaColeccion(Integer cantidad, TipoParticipacion tipoParticipacion, Figurita figurita) {
    this.cantidad = cantidad;
    this.tipoParticipacion = tipoParticipacion;
    this.figurita = figurita;
  }
}
