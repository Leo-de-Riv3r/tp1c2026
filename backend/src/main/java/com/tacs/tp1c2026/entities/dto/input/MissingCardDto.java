package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MissingCardDto {
  @NotBlank(message = "El ID de la figurita es requerido")
  private String cardId;
}
