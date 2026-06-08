package com.tacs.tp1c2026.entities.dto.auction.output;

import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AuctionOfferDto(
    String id,
    String auctionId,
    String bidderUserId,
    String bidderUserName,
    List<ItemDetailDto> offeredItems,
    AuctionOfferStatus status,
    LocalDateTime bidDate,
    Double bidderRating,
    String bidderAvatarId
) {
  public record ItemDetailDto(
      String cardId,
      Integer cardNumber,
      String cardDescription,
      Integer amount
  ) {}
}
