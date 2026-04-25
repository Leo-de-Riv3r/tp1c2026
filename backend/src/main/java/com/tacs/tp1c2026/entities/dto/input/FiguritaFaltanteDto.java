package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.Categoria;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FiguritaFaltanteDto {
  @NotBlank(message = "El ID de la figurita es requerido")
  private String figuritaId;
}
