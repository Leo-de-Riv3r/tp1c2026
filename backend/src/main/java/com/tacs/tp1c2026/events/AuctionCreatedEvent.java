package com.tacs.tp1c2026.events;

import com.tacs.tp1c2026.entities.auction.Auction;
import org.springframework.context.ApplicationEvent;

public record AuctionCreatedEvent(Auction auction){
}
