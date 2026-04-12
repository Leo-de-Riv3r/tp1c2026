package com.tacs.tp1c2026.entities.dto.input;

import lombok.Data;

@Data
public class NuevoFeedbackDto {
  private Integer calificacion;
  private Integer publicacionId;
  private String comentario;
}
