package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.ReglasStrategies.IReglaStrategy;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subastas")
@Getter
public class Subasta {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @OneToOne
  private Figurita figurita;

  @ManyToOne
  private Usuario publicante;

  @Column
  private LocalDateTime fechaCreacion;

  @Column
  private LocalDateTime fechaCierre;

  @OneToMany(mappedBy = "subasta", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<IReglaStrategy> condicionesMinimas = new ArrayList<>();

  @OneToOne
  private OfertaSubasta mejorApuesta;

  @ManyToMany
  @JoinTable(
      name = "subasta_usuarios_interesados",
      joinColumns = @JoinColumn(name = "subasta_id"),
      inverseJoinColumns = @JoinColumn(name = "usuario_id")
  )
  private List<Usuario> usuariosInteresados = new ArrayList<>();

  public Subasta() {}

  public void agregarInteresado(Usuario usuario) {
    if (!this.usuariosInteresados.contains(usuario)) {
      this.usuariosInteresados.add(usuario);
    }
  }

}
