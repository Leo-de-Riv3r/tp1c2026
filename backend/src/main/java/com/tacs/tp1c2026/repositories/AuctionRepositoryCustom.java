package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuctionRepositoryCustom {
  Page<Auction> searchWithFilters(SearchPublicationsFilters filters, Pageable pageable);
}
