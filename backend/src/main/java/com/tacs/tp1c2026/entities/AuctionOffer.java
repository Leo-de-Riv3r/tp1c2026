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
  @Field("subasta")
  private Auction auction;

  @DocumentReference
  private User bidder;

  private List<AuctionItem> offeredItems = new ArrayList<>();

  private AuctionOfferStatus status = AuctionOfferStatus.PENDING;


  public void addItem(AuctionItem offerItem) {
    this.offeredItems.add(offerItem);
  }

  public Integer getTotalStickers() {
    return this.offeredItems.stream()
            .map(AuctionItem::getAmount)
            .reduce(0, Integer::sum);
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


