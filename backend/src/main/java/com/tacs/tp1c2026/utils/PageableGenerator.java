package com.tacs.tp1c2026.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageableGenerator {
  public Pageable buildPageable(Integer page, Integer perPage, int maxPerPage, Sort sort) {
    int validPage = (page == null || page < 1) ? 0 : page - 1;
    int validSize = (perPage == null || perPage < 1) ? 15 : perPage;
    // 3. Límite de seguridad (Si piden 1000, le damos el máximo permitido)
    if (validSize > maxPerPage) {
      validSize = maxPerPage;
    }

    return sort != null ? PageRequest.of(validPage, validSize, sort) : PageRequest.of(validPage, validSize);
  }
}
