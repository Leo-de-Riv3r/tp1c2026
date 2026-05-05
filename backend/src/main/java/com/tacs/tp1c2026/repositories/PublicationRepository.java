package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.exchange.TradePublication;

import java.util.List;

/**
 * Repository for trade publications.
 * `findByPublisherUserId` vive en el custom impl porque `publisherUser` es un
 * @DocumentReference y la persistencia varía entre ObjectId / String según el
 * recorrido del entity — el impl matchea contra ambos.
 */
public interface PublicationRepository extends Repository<TradePublication, String>, PublicationRepositoryCustom {

  List<TradePublication> findByStatus(PublicationStatus status);
}
