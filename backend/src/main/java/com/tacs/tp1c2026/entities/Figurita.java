package com.tacs.tp1c2026.entities;



import java.util.List;

import com.tacs.tp1c2026.entities.enums.Categoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Setter
@Table
@NoArgsConstructor
public class Figurita {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  @Column
  private Integer numero;
  @Column
  private String jugador;
  @Column
  private String descripcion;
  @Column
  private String seleccion;
  @Column
  private String equipo;

  @Column
  @ManyToMany(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinTable(
      name = "figurita_usuarios_faltantes",
      joinColumns = @jakarta.persistence.JoinColumn(name = "figurita_id"),
      inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "usuario_id")
  )
  private List<Usuario> usuariosFaltantes;

  @Enumerated(EnumType.STRING)
  @Column
  private Categoria categoria;
}
