package com.tacs.tp1c2026.repositories.impl;


import com.tacs.tp1c2026.entities.ExchangePublication;
import com.tacs.tp1c2026.entities.dto.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.enums.PublicationState;
import com.tacs.tp1c2026.repositories.ExchangePublicationsRepositoryCustom;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
public class ExchangePublicationsRepositoryImpl implements ExchangePublicationsRepositoryCustom {

  private final MongoTemplate mongoTemplate;

  public ExchangePublicationsRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<ExchangePublication> searchWithFilters(SearchPublicationsFilters filters, Pageable pageable) {

    // 1. Instanciamos una Query limpia
    Query query = new Query();

    query.addCriteria(Criteria.where("state").is(PublicationState.ACTIVA));

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

    List<ExchangePublication> results = mongoTemplate.find(query, ExchangePublication.class);
    
    return PageableExecutionUtils.getPage(
        results,
        pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ExchangePublication.class)
    );
  }
}

