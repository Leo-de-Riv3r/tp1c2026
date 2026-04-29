package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.alert.AlertVisitor;
import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
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
  private Sticker sticker;

  private LocalDateTime closeDate;

  public AuctionClosingAlert(
      Auction auction,
      Sticker sticker,
      LocalDateTime closeDate) {
    this.auction = auction;
    this.sticker = sticker;
    this.closeDate = closeDate;
  }

  @Override
  public AlertaDto visit(AlertVisitor visitor) {
    return visitor.visit(this);
  }

}
