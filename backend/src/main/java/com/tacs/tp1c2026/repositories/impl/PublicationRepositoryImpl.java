package com.tacs.tp1c2026.repositories.impl;

import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.repositories.PublicationRepositoryCustom;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

public class PublicationRepositoryImpl implements PublicationRepositoryCustom {

  private final MongoTemplate mongoTemplate;

  public PublicationRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<TradePublication> searchWithFilters(SearchPublicationsFilters filters, Pageable pageable) {
    Query query = new Query();

    query.addCriteria(Criteria.where("status").is(PublicationStatus.ACTIVE));

    if (filters.getName() != null && !filters.getName().isBlank()) {
      query.addCriteria(Criteria.where("cardDescription").regex(filters.getName(), "i"));
    }

    if (filters.getCountry() != null && !filters.getCountry().isBlank()) {
      query.addCriteria(Criteria.where("cardCountry").regex(filters.getCountry(), "i"));
    }

    if (filters.getTeam() != null && !filters.getTeam().isBlank()) {
      query.addCriteria(Criteria.where("cardTeam").regex(filters.getTeam(), "i"));
    }

    if (filters.getCategory() != null) {
      query.addCriteria(Criteria.where("cardCategory").is(filters.getCategory()));
    }

    if (filters.getCardNumber() != null) {
      query.addCriteria(Criteria.where("cardNumber").is(filters.getCardNumber()));
    }

    query.with(pageable);

    List<TradePublication> results = mongoTemplate.find(query, TradePublication.class);

    return PageableExecutionUtils.getPage(
        results,
        pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), TradePublication.class)
    );
  }

  @Override
  public Page<TradePublication> findByPublisherUserId(String userId, Pageable pageable) {
    // `publisherUser` está persistido como ObjectId (User._id es ObjectId en Mongo
    // aunque el @Id Java sea String). Match directo contra ObjectId; usamos `in`
    // con String + ObjectId por si en algún flujo se persistiera como String.
    Query query = new Query(Criteria.where("publisherUser").in(userId, new ObjectId(userId)));
    query.with(pageable);
    List<TradePublication> results = mongoTemplate.find(query, TradePublication.class);
    return PageableExecutionUtils.getPage(
        results,
        pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), TradePublication.class)
    );
  }
}
