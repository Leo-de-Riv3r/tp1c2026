package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.ExchangePublication;
import com.tacs.tp1c2026.entities.enums.PublicationState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExchangePublicationsRepository extends MongoRepository<ExchangePublication, String>,
    ExchangePublicationsRepositoryCustom
{
  Page<ExchangePublication> findByPublisherUserId(String userId, Pageable pageable);
}
