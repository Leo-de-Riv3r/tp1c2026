package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.alert.AlertVisitor;
import com.tacs.tp1c2026.entities.dto.alert.output.AlertDto;
import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.card.Card;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;


@TypeAlias("auction_near_finish")
@Getter
public class AuctionClosingAlert extends Alert {

  @DocumentReference
  private Auction auction;

  @DocumentReference
  private Card card;

  private LocalDateTime closeDate;

  public AuctionClosingAlert(
      Auction auction,
      Card card,
      LocalDateTime closeDate) {
    this.auction = auction;
    this.card = card;
    this.closeDate = closeDate;
  }

  @Override
  public AlertDto visit(AlertVisitor visitor) {
    return visitor.visit(this);
  }

}
