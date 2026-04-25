package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//usar @Valid en el controller para que se valide
@Data
public class PublicacionIntercambioDto {
  @NotBlank(message = "El ID de la figurita es requerido")
  private String figuritaId;
  @NotNull(message = "La cantidad es requerida")
  private Integer quantity;
}
