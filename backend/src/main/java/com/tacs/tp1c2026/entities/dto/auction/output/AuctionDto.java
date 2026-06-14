package com.tacs.tp1c2026.entities.dto.auction.output;

import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AuctionDto {
  private String id;
  private String cardId;
  private Integer cardNumber;
  private String cardDescription;
  private String cardCountry;
  private String cardTeam;
  private Category cardCategory;
  private LocalDateTime closeDate;
  private AuctionStatus status;
  private BestOfferDto bestOffer;
  // Publisher (denormalized in the entity; rating is resolved live from the User reference)
  private String publisherUserId;
  private String publisherName;
  private String publisherAvatarId;
  private Double publisherRating;
  // Full list of offers in the auction
  private List<AuctionOfferDto> offers;
  private List<AuctionConditionOutputDto> conditions;
}
