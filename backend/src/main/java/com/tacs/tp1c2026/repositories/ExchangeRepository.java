package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.exchange.Exchange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;

public interface ExchangeRepository extends Repository<Exchange, String> {

  /**
   * Devuelve los intercambios donde el usuario participó como A o como B.
   */
  @Query("{ $or: [ { 'userA.userId': ?0 }, { 'userB.userId': ?0 } ] }")
  Page<Exchange> findByUserId(String userId, Pageable pageable);
}
