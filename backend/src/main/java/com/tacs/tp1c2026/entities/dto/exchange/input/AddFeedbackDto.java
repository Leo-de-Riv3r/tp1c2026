package com.tacs.tp1c2026.entities.dto.exchange.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddFeedbackDto {

  @NotNull(message = "El score es requerido")
  @Min(value = 1, message = "El score mínimo es 1")
  @Max(value = 5, message = "El score máximo es 5")
  private Integer score;

  private String comment;
}
