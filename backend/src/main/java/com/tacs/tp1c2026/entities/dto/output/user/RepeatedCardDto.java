package com.tacs.tp1c2026.entities.dto.output.user;

import com.tacs.tp1c2026.entities.dto.output.CardDto;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RepeatedCardDto {
  private CardDto card;
  private Integer quantityForExchange;
  private Integer compromisedInExchange;
  private Integer quantityForAuction;
  private Integer compromisedInAuction;
}
