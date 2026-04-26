package com.tacs.tp1c2026.entities.dto.card.input;

import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import lombok.Data;

@Data
public class FiguritaRepetidaDto {
  private Integer numero;
  private String jugador;
  private String seleccion;
  private String equipo;
  private String descripcion;
  private Categoria categoria;
  private Integer cantidad;
  private TipoParticipacion tipoParticipacion;
}
