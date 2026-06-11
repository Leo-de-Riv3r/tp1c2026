package com.tacs.tp1c2026.entities.auction;


import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@TypeAlias("auction_offer")
@Getter
public class AuctionOffer {
  @Id
  private String id = UUID.randomUUID().toString();

  @DocumentReference
  private User bidder;

  // Snapshot of bidder id+name+rating+avatar — avoids depending on @DocumentReference hydration inside embedded array
  private String bidderId;
  private String bidderName;
  private Double bidderRating;
  private String bidderAvatarId;

  private List<AuctionItem> offeredItems = new ArrayList<>();

  private AuctionOfferStatus status = AuctionOfferStatus.PENDING;

  private LocalDateTime bidDate = LocalDateTime.now();

  protected AuctionOffer() {}

  public AuctionOffer(User bidder, List<AuctionItem> items) {
    this.bidder = bidder;
    this.bidderId = bidder != null ? bidder.getId() : null;
    this.bidderName = bidder != null ? bidder.getName() : null;
    this.bidderRating = bidder != null ? bidder.getRating() : null;
    this.bidderAvatarId = bidder != null ? bidder.getAvatarId() : null;
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

  public void validateCreator(String userId) {
    if (!this.bidder.getId().equals(userId))
      throw new ForbiddenException("Only the offer creator can perform this operation");
  }

  public void cancel() {
    this.status = AuctionOfferStatus.CANCELLED;
  }
}
