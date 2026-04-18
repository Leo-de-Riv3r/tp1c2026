package com.tacs.tp1c2026.entities;

import jakarta.persistence.CascadeType;
import jakarta.annotation.PostConstruct;
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
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table
public class Usuario {
  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  @Column
  private String nombre;
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "figurita_coleccion_id", referencedColumnName = "id")
  private List<FiguritaColeccion> repetidas = new ArrayList<>();

  @Column
  private LocalDateTime fechaAlta = LocalDateTime.now();

  @ManyToMany(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinTable(
      name = "usuario_faltantes",
      joinColumns = @JoinColumn(name = "usuario_id"),
      inverseJoinColumns = @JoinColumn(name = "figurita_id")
  )
  private List<Figurita> faltantes = new ArrayList<>();

  @OneToMany(mappedBy = "usuario")
  private List<Alerta> alertas = new ArrayList<>();

  @OneToMany
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private List<Usuario> sugerenciasIntercambios = new ArrayList<>();

  @Transient
  private VectorProfile vectorProfile;

  public void agregarSugerencia(Usuario sugerencias) {
    this.sugerenciasIntercambios.add(sugerencias);
  }

  public void agregarRepetidas(FiguritaColeccion figuritaColeccion) {
    this.repetidas.add(figuritaColeccion);
    this.vectorProfile.addCard(figuritaColeccion);
  }

  public void agregarFaltantes(Figurita figurita) {
    this.faltantes.add(figurita);
    this.vectorProfile.addCard(figurita);
  }

  public void agregarAlerta(Alerta alerta) {
    this.alertas.add(alerta);
  }

  public VectorProfile getVectorProfile() {;
    return this.vectorProfile;
  }

  @PostConstruct
  public void initializeVectorProfile() {
    this.vectorProfile = new VectorProfile(this.repetidas, this.faltantes);
  }

  public void removerSugerencias() {
    this.sugerenciasIntercambios.clear();
  }
}
