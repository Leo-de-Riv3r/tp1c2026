package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.CardCategory;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PublicacionDto {
  private Integer id;
  private Integer numero;
  private String descripcion;
  private String jugador;
  private String seleccion;
  private String equipo;
  private CardCategory cardCategory;
  private Integer cantidad;
}
