package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.CardCategory;
import lombok.AllArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Setter
public class FiguritaDto {
  private Integer numero;
  private String descripcion;
  private String jugador;
  private String seleccion;
  private String equipo;
  private CardCategory cardCategory;
}
