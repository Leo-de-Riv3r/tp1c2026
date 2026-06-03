package com.tacs.tp1c2026.events;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.user.User;
import org.springframework.context.ApplicationEvent;

public record UserInterestedInActionEvent (User user, Auction auction) {
}
