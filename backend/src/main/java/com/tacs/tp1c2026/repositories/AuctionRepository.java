package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Repository for Auction documents.
 */
public interface AuctionRepository extends Repository<Auction, String>, AuctionRepositoryCustom {

  Page<Auction> findByPublisherUserId(String userId, Pageable pageable);

  List<Auction> findByStatus(AuctionStatus status);
}
