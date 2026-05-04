package com.tacs.tp1c2026.entities.auction;


import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import com.tacs.tp1c2026.entities.user.User;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
public class AuctionOffer {

  @Id
  private String id = UUID.randomUUID().toString();

  @DocumentReference
  private final User bidder;

  private List<AuctionItem> offeredItems = new ArrayList<>();

  private AuctionOfferStatus status = AuctionOfferStatus.PENDING;

  private final LocalDateTime bidDate = LocalDateTime.now();


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
