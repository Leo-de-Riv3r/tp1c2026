package com.tacs.tp1c2026.entities.auction;


import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import com.tacs.tp1c2026.entities.user.User;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.ArrayList;
import java.util.List;


public class AuctionOffer {

  @Id
  private Integer id;

  @DocumentReference
  private final User bidder;

  private List<AuctionItem> offeredItems = new ArrayList<>();

  private AuctionOfferStatus status = AuctionOfferStatus.PENDING;


  public AuctionOffer(User bidder, List<AuctionItem> items) {
    this.bidder = bidder;
    this.offeredItems = new ArrayList<>(items == null ? List.of() : items);
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


