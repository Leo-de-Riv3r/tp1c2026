package com.tacs.tp1c2026.entities.dto.trade.input;

import com.tacs.tp1c2026.entities.enums.ReviewAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewProposalDto {
  private String publicationId;
  private String proposalId;

  @NotNull(message = "La acción es requerida")
  private ReviewAction action;
}
