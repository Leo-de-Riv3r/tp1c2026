package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewFeedbackDto {
  @NotBlank(message = "Se requiere un ID de publicacion")
  private String publicacionId;
  @NotBlank(message = "Se requiere un comentario")
  @Size(max = 150, message = "El comentario no puede superar los 255 caracteres")
  private String commentary;
  @NotNull(message = "Se requiere una calificación")
  @Min(value = 1, message = "La calificación mínima es 1")
  @Max(value = 5, message = "La calificación máxima es 5")
  private Integer rating;
}
