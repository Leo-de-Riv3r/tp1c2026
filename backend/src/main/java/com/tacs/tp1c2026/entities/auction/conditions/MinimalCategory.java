package com.tacs.tp1c2026.entities.auction.conditions;

import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.user.User;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("minimal_category")
public class MinimalCategory extends AuctionCondition {
  private Category category;

  protected MinimalCategory() {}

  public MinimalCategory(Category value) {
    this.category = value;
  }

  public Category getCategory() { return category; }

  @Override
  public boolean canOffer(User user, AuctionOffer offer) {
    //check that offered cards are equal or better quality
    return offer.getOfferedItems().stream()
        .allMatch(item -> item.getCard().getCategory().ordinal() >= this.category.ordinal());
  }
}
