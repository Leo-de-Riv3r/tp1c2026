package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.ExchangePublication;
import com.tacs.tp1c2026.entities.dto.input.SearchPublicationsFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExchangePublicationsRepositoryCustom {
  Page<ExchangePublication> searchWithFilters(SearchPublicationsFilters filters, Pageable pageable);
}
