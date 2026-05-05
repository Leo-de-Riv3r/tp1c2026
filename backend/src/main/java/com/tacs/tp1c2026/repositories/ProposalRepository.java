package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Repository for trade proposals (top-level collection "proposals").
 * `publication`, `proposerUser` y `receiver` se persisten como ObjectId vía
 * @DocumentReference; matcheamos con `$in [string, ObjectId]` para tolerar
 * cualquiera de las dos representaciones que use Spring al persistir.
 */
public interface ProposalRepository extends Repository<TradeProposal, String> {

  @Query("{ 'publication': { $in: [ ?0, ?1 ] } }")
  List<TradeProposal> findByPublicationIdAny(String pubIdString, ObjectId pubIdObjectId);

  default List<TradeProposal> findByPublicationId(String pubId) {
    return findByPublicationIdAny(pubId, new ObjectId(pubId));
  }

  @Query("{ 'publication': { $in: [ ?0, ?1 ] }, 'status': ?2 }")
  List<TradeProposal> findByPublicationIdAndStatusAny(String pubIdString, ObjectId pubIdObjectId, TradeProposalStatus status);

  default List<TradeProposal> findByPublicationIdAndStatus(String pubId, TradeProposalStatus status) {
    return findByPublicationIdAndStatusAny(pubId, new ObjectId(pubId), status);
  }

  @Query("{ 'proposerUser': { $in: [ ?0, ?1 ] } }")
  List<TradeProposal> findByProposerUserIdAny(String userIdString, ObjectId userIdObjectId);

  default List<TradeProposal> findByProposerUserId(String userId) {
    return findByProposerUserIdAny(userId, new ObjectId(userId));
  }

  @Query("{ 'receiver': { $in: [ ?0, ?1 ] } }")
  List<TradeProposal> findByReceiverIdAny(String userIdString, ObjectId userIdObjectId);

  default List<TradeProposal> findByReceiverId(String userId) {
    return findByReceiverIdAny(userId, new ObjectId(userId));
  }
}
