package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class NewExchangeProposalDto {
  private String publicationId;

  @NotEmpty(message = "La lista de IDs de figuritas es requerida")
  private List<String> cardsIds;
}
