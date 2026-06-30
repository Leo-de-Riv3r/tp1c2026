package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para documentos Auction.
 * `findByPublisherUserId` vive en el custom impl porque `publisherUser` es un
 * @DocumentReference (escalar con ObjectId, no sub-doc) — la query derivada falla.
 */
public interface AuctionRepository extends Repository<Auction, String>, AuctionRepositoryCustom {

  List<Auction> findByStatus(AuctionStatus status);
  List<Auction> findByStatusAndCloseDateBetween(
      AuctionStatus status,
      LocalDateTime from,
      LocalDateTime to
  );

  long countByStatus(AuctionStatus status);

  /** Auctions creadas en el rango {@code [startInclusive, endExclusive)}. Usado por el snapshot diario y el delta del día en curso. */
  @Query(value = "{ 'creationDate': { $gte: ?0, $lt: ?1 } }", count = true)
  long countCreatedBetween(LocalDateTime startInclusive, LocalDateTime endExclusive);
}
