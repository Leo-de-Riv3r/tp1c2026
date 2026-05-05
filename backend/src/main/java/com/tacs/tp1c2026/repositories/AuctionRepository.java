package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;

import java.util.List;

/**
 * Repository for Auction documents.
 * `findByPublisherUserId` vive en el custom impl porque `publisherUser` es un
 * @DocumentReference (escalar con ObjectId, no sub-doc) — la query derivada falla.
 */
public interface AuctionRepository extends Repository<Auction, String>, AuctionRepositoryCustom {

  List<Auction> findByStatus(AuctionStatus status);
}
