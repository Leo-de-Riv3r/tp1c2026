package com.tacs.tp1c2026.entities.auction.conditions;

import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.OfferRankingMetric;
import com.tacs.tp1c2026.entities.user.User;

/**
 * Condición que requiere una cantidad mínima de figuritas para adjudicar una subasta a un ofertante.
 */
public class MinimalCardCount extends AuctionCondition {
    private Integer count;

    protected MinimalCardCount() {}

    public MinimalCardCount(Integer count) {
        this.count = count;
    }

    public Integer getCount() {
        return this.count;
    }

  @Override
  public boolean canOffer(User user, AuctionOffer offer) {
    return count <= offer.getOfferedItems().stream().mapToInt(item -> item.getAmount()).sum();
  }

  @Override
  public OfferRankingMetric rankingMetric() {
    return OfferRankingMetric.QUANTITY;
  }
}

