package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.ReviewAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewProposalDto {
  private String publicationId;
  private String proposalId;
  @NotNull(message = "La accion es requerida")
  private ReviewAction action;
}
