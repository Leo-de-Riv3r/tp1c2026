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

import java.util.List;

import lombok.Getter;

@Getter
@Entity
@Table
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @Column
  private String nombre;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "figurita_coleccion_id", referencedColumnName = "id")
  private List<FiguritaColeccion> repetidas;

  @Column
  private int groupId;

  @OneToMany
  @JoinColumn(name = "figurita_id", referencedColumnName = "id")
  private List<Figurita> faltantes;

  @OneToMany
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private List<Usuario> sugerenciasIntercambios;

  public void agregarRepetidas(FiguritaColeccion figuritaColeccion) {
    this.repetidas.add(figuritaColeccion);
  }

  public void agregarFaltantes(Figurita figurita) {
    this.faltantes.add(figurita);
  }

  public void agregarSugerencia(Usuario sugerencias) {
    this.sugerenciasIntercambios.add(sugerencias);
  }

  public void removerSugerencias() {
    this.sugerenciasIntercambios.clear();
  }

  public VectorProfile getVectorProfile() {
    VectorProfile.Builder builder = new VectorProfile.Builder();

    if (this.faltantes != null) {
      for (Figurita figurita : this.faltantes) {
        builder.set(figurita.getId(), 1);
      }
    }

    if (this.repetidas != null) {
      for (FiguritaColeccion figuritaColeccion : this.repetidas) {
        builder.set(figuritaColeccion.getFigurita().getId(), -1);
      }
    }

    return builder.build();
  }

}
