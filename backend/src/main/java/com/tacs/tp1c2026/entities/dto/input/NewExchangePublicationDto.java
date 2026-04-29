package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//usar @Valid en el controller para que se valide
@Data
public class NewExchangePublicationDto {
  @NotBlank(message = "El ID de la card es requerido")
  private String cardId;
  @NotNull(message = "La cantidad es requerida")
  private Integer quantity;
}
