package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class Subasta {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "figurita_coleccion_id", referencedColumnName = "id")
  private FiguritaColeccion figuritaPublicada;

  @ManyToOne
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private Usuario usuarioPublicante;

  @Column
  private LocalDateTime fechaCreacion;

  @Column
  private LocalDateTime fechaCierre;

  @Column
  private Integer cantidadMinFiguritas;

  @Enumerated(EnumType.STRING)
  @Column
  private EstadoSubasta estado = EstadoSubasta.ACTIVA;

  @ManyToOne
  @JoinColumn(name = "mejor_oferta_id", referencedColumnName = "id")
  private OfertaSubasta mejorOferta;

  @OneToMany(mappedBy = "subasta")
  private List<OfertaSubasta> ofertas;
}
