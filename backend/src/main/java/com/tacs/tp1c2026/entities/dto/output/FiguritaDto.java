package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.Categoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;

@AllArgsConstructor
@Setter
public class FiguritaDto {
  private Integer numero;
  private String descripcion;
  private String jugador;
  private String seleccion;
  private String equipo;
  private Categoria categoria;
}
