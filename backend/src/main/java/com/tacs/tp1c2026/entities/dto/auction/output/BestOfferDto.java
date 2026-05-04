package com.tacs.tp1c2026.entities.dto.auction.output;

import com.tacs.tp1c2026.entities.dto.card.output.CardDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BestOfferDto {
  private String username;
  private List<CardDTO> cards;
}
