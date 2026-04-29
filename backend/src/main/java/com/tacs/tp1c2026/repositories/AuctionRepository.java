package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Auction;
import com.tacs.tp1c2026.entities.enums.AuctionState;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuctionRepository extends MongoRepository<Auction, String>, AuctionRepositoryCustom {
   List<Auction> findByState(AuctionState state);
   Page<Auction> findByPublisherUserId(String userId, Pageable pageable);
}