package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.ReviewAction;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewProposalDto {
  @NotBlank(message = "El id de publicacion es requerido")
  private String publicacionId;
  @NotBlank(message = "El id de la propuesta es requerido")
  private String propuestaId;
  @NotBlank(message = "La accion es requerida")
  private ReviewAction action;
}
