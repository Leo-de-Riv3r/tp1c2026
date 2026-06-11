package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicationRepositoryCustom {
  Page<TradePublication> searchWithFilters(SearchPublicationsFilters filters, Pageable pageable);

  Page<TradePublication> findByPublisherUserId(String userId, Pageable pageable);

  /**
   * Atómicamente verifica que haya cupos (remainingCount - pendingCount >= requested) e
   * incrementa pendingCount + version en un solo findAndModify. Retorna false si no hay cupos
   * o la publicación no está activa.
   */
  boolean tryReserveSlots(String publicationId, int requested);

  /**
   * Atómicamente decrementa pendingCount + version cuando una proposal es rechazada o cancelada.
   * Solo actúa si pendingCount >= amount (previene underflow en docs legacy sin el campo).
   */
  void releaseSlots(String publicationId, int amount);
}
