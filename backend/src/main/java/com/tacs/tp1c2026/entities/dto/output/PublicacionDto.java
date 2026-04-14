package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.Categoria;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PublicacionDto {
  private Integer id;
  private Integer numero;
  private String descripcion;
  private String jugador;
  private String seleccion;
  private String equipo;
  private Categoria categoria;
  private Integer cantidad;
}
