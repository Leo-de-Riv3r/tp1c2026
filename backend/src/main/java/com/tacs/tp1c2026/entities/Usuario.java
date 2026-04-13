package com.tacs.tp1c2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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

  @OneToMany
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

  @Transient
  private VectorProfile vectorProfile = VectorProfile.empty();

  public void agregarRepetidas(FiguritaColeccion figuritaColeccion) {
    this.repetidas.add(figuritaColeccion);
    this.sincronizarPerfilVector();
  }

  public void agregarFaltantes(Figurita figurita) {
    this.faltantes.add(figurita);
    this.sincronizarPerfilVector();
  }

  public void agregarSugerencia(Usuario sugerencias) {
    this.sugerenciasIntercambios.add(sugerencias);
  }

  public void removerSugerencias() {
    this.sugerenciasIntercambios.clear();
  }

  @PostLoad
  @PostPersist
  @PostUpdate
  private void sincronizarPerfilVector() {
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

    this.vectorProfile = builder.build();
  }

  public VectorProfile getVectorProfile() {
    if (this.vectorProfile == null) {
      sincronizarPerfilVector();
    }
    return this.vectorProfile;
  }

}
