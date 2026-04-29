package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;
import lombok.Setter;

@Setter
public class BestOfferDto {
  private String username;
  private List<CardDto> cards;
}
