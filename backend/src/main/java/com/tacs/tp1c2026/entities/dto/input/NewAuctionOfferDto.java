package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class NewAuctionOfferDto {
  private String auctionId;
  @NotBlank(message = "La lista de IDs de figuritas es requerida")
  private List<String> cardsIds;
}
