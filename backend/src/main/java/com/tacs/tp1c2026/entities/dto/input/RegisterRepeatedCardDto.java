package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.ParticipationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRepeatedCardDto {
  @NotBlank(message = "El ID de la card es requerido")
  private String cardId;
  @NotNull(message = "La cantidad es requerida")
  private Integer quantity;
  @NotNull(message = "El tipo de participación es requerido")
  private ParticipationType participationType;
}
