package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Repository for trade publications.
 */
public interface PublicationRepository extends Repository<TradePublication, String>, PublicationRepositoryCustom {

  Page<TradePublication> findByPublisherUserId(String userId, Pageable pageable);

  List<TradePublication> findByStatus(PublicationStatus status);
}
