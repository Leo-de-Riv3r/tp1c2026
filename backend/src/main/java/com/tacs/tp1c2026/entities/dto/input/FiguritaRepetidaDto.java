package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;


public record FiguritaRepetidaDto(
  Integer numero,
  String jugador,
  String seleccion,
  String equipo,
  String descripcion,
  Categoria categoria,
  Integer cantidad,
  TipoParticipacion tipoParticipacion
) {}