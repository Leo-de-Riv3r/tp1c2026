package com.tacs.tp1c2026.entities.auction;

import com.tacs.tp1c2026.entities.card.Card;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


@Getter
public class AuctionItem {

  @Id
  private Integer id;

  @DocumentReference
  private Card card;

  private Integer amount;

  public AuctionItem(Card card, Integer amount) {
    this.card = card;
    this.amount = amount;
  }

}
