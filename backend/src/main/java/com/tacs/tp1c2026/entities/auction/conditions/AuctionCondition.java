package com.tacs.tp1c2026.entities.auction.conditions;

import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.user.User;
import org.springframework.data.annotation.TypeAlias;

/**
 * Base class for auction conditions. Concrete conditions (e.g. minimal sticker count)
 * should extend this class. Kept minimal for now.
 */
@TypeAlias("auction_condition")
public abstract class AuctionCondition {
  public abstract boolean canOffer(User user, AuctionOffer offer);
}

