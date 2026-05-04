package com.tacs.tp1c2026.entities.dto.auction.output;

import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuctionDto {
  private String id;
  private Integer cardNumber;
  private String cardDescription;
  private String cardCountry;
  private String cardTeam;
  private Category cardCategory;
  private LocalDateTime closeDate;
  private AuctionStatus status;
  private BestOfferDto bestOffer;
}
