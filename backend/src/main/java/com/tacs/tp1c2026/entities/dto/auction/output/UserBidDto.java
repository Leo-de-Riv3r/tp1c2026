package com.tacs.tp1c2026.entities.dto.auction.output;

import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Flat view for "My Offers": one entry per offer made by the user,
 * with the context of the auction it belongs to.
 */
@Getter
@AllArgsConstructor
public class UserBidDto {
  private String auctionId;
  // Card published in the auction
  private String cardId;
  private Integer cardNumber;
  private String cardDescription;
  private String cardCountry;
  private String cardTeam;
  // Auction publisher
  private String publisherUserId;
  private String publisherName;
  // Auction status
  private AuctionStatus auctionStatus;
  private LocalDateTime closeDate;
  // User's offer data
  private String offerId;
  private List<OfferItemDto> offeredItems;
  private AuctionOfferStatus offerStatus;
  private LocalDateTime bidDate;

  @Getter
  @AllArgsConstructor
  public static class OfferItemDto {
    private String cardId;
    private Integer cardNumber;
    private String cardDescription;
    private Integer amount;
  }
}
