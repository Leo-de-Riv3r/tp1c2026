package com.tacs.tp1c2026.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {
  @Id
  @GeneratedValue
  private Integer id;
  private String nombre;
  private List<Publicacion> repetidas;
  private List<Figurita> faltantes;

  public Usuario(Integer id, String nombre, List<Publicacion> repetidas, List<Figurita> faltantes) {
        this.id = id;
        this.nombre = nombre;
        this.repetidas = repetidas;
        this.faltantes = faltantes;
    }

  public void addFiguritaColeccion(Publicacion figurita) {
        this.repetidas.add(figurita);
  }

}