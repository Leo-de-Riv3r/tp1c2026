package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.ParticipationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveCardsRequestDto {
  @NotBlank(message = "Se requiere un id de figurita")
  private String cardId;
  @NotNull(message = "Se requiere una cantidad")
  @Min(value = 1, message ="La cantidad debe ser al menos 1" )
  private Integer quantity;
  @NotNull(message = "Se requiere un tipo de participación")
  private ParticipationType newParticipationType;
}
