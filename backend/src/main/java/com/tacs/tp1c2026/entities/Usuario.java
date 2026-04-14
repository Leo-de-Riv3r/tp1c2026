package com.tacs.tp1c2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
@Entity
@Table
public class Usuario {
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  @Column
  private String nombre;
  @OneToMany
  @JoinColumn(name = "figurita_coleccion_id", referencedColumnName = "id")
  private List<FiguritaColeccion> repetidas;

  @ManyToMany(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinTable(
      name = "usuario_faltantes",
      joinColumns = @JoinColumn(name = "usuario_id"),
      inverseJoinColumns = @JoinColumn(name = "figurita_id")
  )
  private List<Figurita> faltantes;

  @OneToMany(mappedBy = "usuario")
  private List<Alerta> alertas = new ArrayList<>();

  public void agregarRepetidas(FiguritaColeccion figuritaColeccion) {
    this.repetidas.add(figuritaColeccion);
  }

  public void agregarFaltantes(Figurita figurita) {
    this.faltantes.add(figurita);
  }

  public void agregarAlerta(Alerta alerta) {
    this.alertas.add(alerta);
  }
}
