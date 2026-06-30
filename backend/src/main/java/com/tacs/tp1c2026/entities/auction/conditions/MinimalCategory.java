package com.tacs.tp1c2026.entities.auction.conditions;

import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.OfferRankingMetric;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.user.User;

public class MinimalCategory extends AuctionCondition {
  private Category category;

  protected MinimalCategory() {}

  public MinimalCategory(Category value) {
    this.category = value;
  }

  public Category getCategory() { return category; }

  @Override
  public OfferRankingMetric rankingMetric() {
    return OfferRankingMetric.RARITY;
  }

  @Override
  public boolean canOffer(User user, AuctionOffer offer) {
    //verifica que las cards ofrecidas sean de calidad igual o superior
    return offer.getOfferedItems().stream()
        .allMatch(item -> item.getCard().getCategory().ordinal() >= this.category.ordinal());
  }
}
