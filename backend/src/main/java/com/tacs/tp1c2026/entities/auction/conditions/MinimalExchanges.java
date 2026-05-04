package com.tacs.tp1c2026.entities.auction.conditions;

import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.user.User;

public class MinimalExchanges extends AuctionCondition{
  private Integer quantity;

  public MinimalExchanges(Integer quantity) {
    this.quantity = quantity;
  }

  @Override
  public boolean canOffer(User user, AuctionOffer offer) {
    return user.getExchangesCount() >= quantity;
  }
}
