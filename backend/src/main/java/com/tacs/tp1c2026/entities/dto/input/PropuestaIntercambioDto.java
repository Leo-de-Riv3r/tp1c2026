package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class PropuestaIntercambioDto {
  @NotBlank(message = "El ID de la figurita es requerido")
  private String publicacionId;

  @NotEmpty(message = "La lista de IDs de figuritas es requerida")
  private List<String> idFiguritas;
}
