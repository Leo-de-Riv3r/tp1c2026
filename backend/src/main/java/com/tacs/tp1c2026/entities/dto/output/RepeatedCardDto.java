package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.CardCategory;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RepeatedCardDto {
  private CardDto card;
  private Integer quantityForExchange;
  private Integer compromisedInExchange;
  private Integer quantityForAuction;
  private Integer compromisedInAuction;
}
