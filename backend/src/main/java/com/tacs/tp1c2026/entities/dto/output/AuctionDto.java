package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.CardCategory;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AuctionDto {
  private String id;
  private String name;
  private String team;
  private String country;
  private CardCategory category;
  private LocalDateTime finishDate;
  private Integer minCardsRequired;
  private BestOfferDto bestOffer;
}

