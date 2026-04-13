package com.tacs.tp1c2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

import com.tacs.tp1c2026.entities.bucket.BucketManager;

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

  @Column
  @Convert(converter = ShortArrayConverter.class)
  private short[] vector = new short[BucketManager.CANTIDAD_FIGURITAS];

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
    vector[figuritaColeccion.getFigurita().getId()] = 1;
    BucketManager.getInstance().updateBuckets(this, vector);
  }

  public void agregarFaltantes(Figurita figurita) {
    this.faltantes.add(figurita);
    vector[figurita.getId()] = -1;
    BucketManager.getInstance().updateBuckets(this, vector);
  }

  public short[] getQueryVector() {
    short[] queryVector = new short[BucketManager.CANTIDAD_FIGURITAS];
    for (int i = 0; i < BucketManager.CANTIDAD_FIGURITAS; i++) {
      queryVector[i] = (short) (vector[i] * -1);
    }
    return queryVector;
  }



}
