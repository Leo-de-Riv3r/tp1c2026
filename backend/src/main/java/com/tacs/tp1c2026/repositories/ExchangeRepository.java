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

  /**
   * Exchanges donde el usuario fue A y recibió feedback (feedbackFromB != null).
   * Usado para recalcular el rating del usuario reviewado.
   */
  java.util.List<Exchange> findByUserAUserIdAndFeedbackFromBNotNull(String userId);

  /**
   * Exchanges donde el usuario fue B y recibió feedback (feedbackFromA != null).
   * Usado para recalcular el rating del usuario reviewado.
   */
  java.util.List<Exchange> findByUserBUserIdAndFeedbackFromANotNull(String userId);

  /** Exchanges creados en el rango {@code [startInclusive, endExclusive)}. Usado por el snapshot diario y el delta del día en curso. */
  @Query(value = "{ 'createdAt': { $gte: ?0, $lt: ?1 } }", count = true)
  long countCreatedBetween(java.time.LocalDateTime startInclusive, java.time.LocalDateTime endExclusive);

  /** Exchanges creados después de {@code cutoff}, sin paginar. Usado para top-N agregados in-memory en Capa 3. */
  java.util.List<Exchange> findByCreatedAtAfter(java.time.LocalDateTime cutoff);
}
