package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;


public class AuctionOffer {

  @Id
  private Integer id;

  @DocumentReference
  private final User bidder;

  private List<AuctionItem> offeredItems = new ArrayList<>();

  private AuctionOfferStatus status = AuctionOfferStatus.PENDING;

  public void reserveItems() throws com.tacs.tp1c2026.exceptions.InsufficientStickerException {
    for (AuctionItem item : this.offeredItems) {
      if (!this.bidder.hasEnoughForAuction(item.getSticker(), item.getAmount())) {
        throw new com.tacs.tp1c2026.exceptions.InsufficientStickerException("Insufficient stickers for auction");
      }
    }
    for (AuctionItem item : this.offeredItems) {
      this.bidder.removeAuctionStickers(item.getSticker(), item.getAmount());
    }
  }

  public AuctionOffer(User bidder, List<AuctionItem> items) {
    this.bidder = bidder;
    this.offeredItems = new java.util.ArrayList<>(items == null ? java.util.List.of() : items);
  }

  public boolean isPending() {
    return AuctionOfferStatus.PENDING.equals(this.status);
  }

  public void accept() {
    this.status = AuctionOfferStatus.ACCEPTED;
  }

  public void reject() {
    this.status = AuctionOfferStatus.REJECTED;
  }
}


