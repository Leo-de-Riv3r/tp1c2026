package com.tacs.tp1c2026.entities;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


@Getter
public class AuctionItem {

  @Id
  private Integer id;

  @DocumentReference
  private Sticker sticker;

  private Integer amount;

  public AuctionItem(Sticker sticker, Integer amount) {
    this.sticker = sticker;
    this.amount = amount;
  }

}
