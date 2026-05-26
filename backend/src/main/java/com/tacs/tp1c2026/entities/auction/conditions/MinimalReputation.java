package com.tacs.tp1c2026.entities.auction.conditions;

import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.user.User;

public class MinimalReputation extends AuctionCondition{
  private Integer reputation;

  protected MinimalReputation() {}

  public MinimalReputation(Integer quantity) {
    this.reputation = quantity;
  }

  public Integer getReputation() { return reputation; }

  @Override
  public boolean canOffer(User user, AuctionOffer offer) {
    return user.getRating() >= this.reputation;
  }
}
