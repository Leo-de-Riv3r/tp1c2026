package com.tacs.tp1c2026.entities.auction.conditions;

import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.user.User;

public class MinimalCategory extends AuctionCondition {
  private Category category;
  public MinimalCategory(Category value) {
    //map value to a catreogry from enum, if not mappeable, throw exception
    this.category = value;
  }

  @Override
  public boolean canOffer(User user, AuctionOffer offer) {
    //check that offered cards are equal or better quality
    return offer.getOfferedItems().stream()
        .allMatch(item -> item.getCard().getCategory().ordinal() >= this.category.ordinal());
  }
}
