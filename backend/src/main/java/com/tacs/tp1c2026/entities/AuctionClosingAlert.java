package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


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
