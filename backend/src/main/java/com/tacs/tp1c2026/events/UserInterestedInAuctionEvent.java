package com.tacs.tp1c2026.events;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.user.User;

public record UserInterestedInAuctionEvent(User user, Auction auction) {
}
