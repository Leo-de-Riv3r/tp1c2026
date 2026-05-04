package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicationRepositoryCustom {
  Page<TradePublication> searchWithFilters(SearchPublicationsFilters filters, Pageable pageable);
}
