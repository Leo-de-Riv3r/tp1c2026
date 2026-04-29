package com.tacs.tp1c2026.repositories.impl;

import com.tacs.tp1c2026.entities.Auction;
import com.tacs.tp1c2026.entities.dto.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.enums.AuctionState;
import com.tacs.tp1c2026.repositories.AuctionRepositoryCustom;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

public class AuctionRepositoryImpl implements AuctionRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  public AuctionRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<Auction> searchWithFilters(SearchPublicationsFilters filters, Pageable pageable) {

    // 1. Instanciamos una Query limpia
    Query query = new Query();

    query.addCriteria(Criteria.where("state").is(AuctionState.ACTIVA));

    if (filters.getName() != null && !filters.getName().isBlank()) {
      // "i" es para ignorar mayúsculas y minúsculas (Case Insensitive Regex)
      query.addCriteria(Criteria.where("name").regex(filters.getName(), "i"));
    }

    if (filters.getCountry() != null && !filters.getCountry().isBlank()) {
      query.addCriteria(Criteria.where("country").regex(filters.getCountry(), "i"));
    }

    if (filters.getTeam() != null && !filters.getTeam().isBlank()) {
      query.addCriteria(Criteria.where("team").regex(filters.getTeam(), "i"));
    }

    if (filters.getCategory() != null) {
      query.addCriteria(Criteria.where("category").is(filters.getCategory()));
    }

    query.with(pageable);

    List<Auction> results = mongoTemplate.find(query, Auction.class);

    return PageableExecutionUtils.getPage(
        results,
        pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Auction.class)
    );
  }
}
