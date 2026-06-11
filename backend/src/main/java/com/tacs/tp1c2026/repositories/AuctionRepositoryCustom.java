package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuctionRepositoryCustom {
  Page<Auction> searchWithFilters(SearchPublicationsFilters filters, Pageable pageable);

  Page<Auction> findByPublisherUserId(String userId, Pageable pageable);

  /** Auctions where the user has at least one offer. */
  List<Auction> findByOffersBidderId(String userId);
}
