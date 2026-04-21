/*
package com.tacs.tp1c2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table
@Setter
@Getter
public class Feedback {
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  @ManyToOne
  @JoinColumn(name = "calificador_id", referencedColumnName = "id")
  private Usuario calificador;

  @ManyToOne
  @JoinColumn(name = "publicacion_id", referencedColumnName = "id")
  private PublicacionIntercambio publicacionIntercambio;
  @Column
  private Integer calificacion;
  @Column
  private String comentario;
  @Column
  private LocalDateTime fecha = LocalDateTime.now();
}
*/
