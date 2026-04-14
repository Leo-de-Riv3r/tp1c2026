package com.tacs.tp1c2026.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table
@Setter
public class Usuario {
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  @Column
  private String nombre;
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "figurita_coleccion_id", referencedColumnName = "id")
  private List<FiguritaColeccion> repetidas = new ArrayList<>();
  @OneToMany
  @JoinColumn(name = "figurita_id", referencedColumnName = "id")
  private List<Figurita> faltantes = new ArrayList<>();

  @Column
  private LocalDateTime fechaAlta = LocalDateTime.now();
  public void agregarRepetidas(FiguritaColeccion figuritaColeccion) {
    this.repetidas.add(figuritaColeccion);
  }

  public void agregarFaltantes(Figurita figurita) {
    this.faltantes.add(figurita);
  }
}
