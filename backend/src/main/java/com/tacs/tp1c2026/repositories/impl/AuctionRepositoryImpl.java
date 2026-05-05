package com.tacs.tp1c2026.repositories.impl;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.repositories.AuctionRepositoryCustom;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

public class AuctionRepositoryImpl implements AuctionRepositoryCustom {

  private final MongoTemplate mongoTemplate;

  public AuctionRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<Auction> searchWithFilters(SearchPublicationsFilters filters, Pageable pageable) {
    Query query = new Query();

    query.addCriteria(Criteria.where("status").is(AuctionStatus.ACTIVE));

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

    if(filters.getCardType() != null) {
      query.addCriteria(Criteria.where("cardType").is(filters.getCardType()));
    }

    query.with(pageable);

    List<Auction> results = mongoTemplate.find(query, Auction.class);

    return PageableExecutionUtils.getPage(
        results,
        pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Auction.class)
    );
  }

  @Override
  public Page<Auction> findByPublisherUserId(String userId, Pageable pageable) {
    // `publisherUser` está persistido como ObjectId vía @DocumentReference;
    // matcheamos in(string, ObjectId) para tolerar cualquiera de las dos formas.
    Query query = new Query(Criteria.where("publisherUser").in(userId, new ObjectId(userId)));
    query.with(pageable);
    List<Auction> results = mongoTemplate.find(query, Auction.class);
    return PageableExecutionUtils.getPage(
        results,
        pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Auction.class)
    );
  }

  @Override
  public List<Auction> findByOffersBidderId(String userId) {
    // `offers[].bidder` es @DocumentReference dentro de un array embebido — la persistencia
    // varía y la query directa por path no es confiable. Filtramos en memoria
    // (datasets chicos por ahora; escalar con índice secundario si crece).
    return mongoTemplate.findAll(Auction.class).stream()
        .filter(a -> a.getOffers() != null && a.getOffers().stream()
            .anyMatch(o -> o.getBidder() != null
                && java.util.Objects.equals(o.getBidder().getId(), userId)))
        .toList();
  }
}
