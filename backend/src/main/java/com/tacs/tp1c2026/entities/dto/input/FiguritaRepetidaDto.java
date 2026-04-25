package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FiguritaRepetidaDto {
  @NotBlank(message = "El ID de la figurita es requerido")
  private String figuritaId;
  @NotBlank(message = "La cantidad es requerida")
  private Integer cantidad;
  @NotBlank(message = "El tipo de participación es requerido")
  private TipoParticipacion tipoParticipacion;
}
