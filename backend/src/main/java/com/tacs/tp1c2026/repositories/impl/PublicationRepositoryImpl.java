package com.tacs.tp1c2026.repositories.impl;

import com.tacs.tp1c2026.entities.dto.common.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.enums.PublicationStatus;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.repositories.PublicationRepositoryCustom;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    // `publisherUser` is persisted as ObjectId (User._id is ObjectId in Mongo
    // even though the Java @Id is String). Direct match against ObjectId; use `in`
    // with String + ObjectId in case some flow persists it as String.
    Query query = new Query(Criteria.where("publisherUser").in(userId, new ObjectId(userId)));
    query.with(pageable);
    List<TradePublication> results = mongoTemplate.find(query, TradePublication.class);
    return PageableExecutionUtils.getPage(
        results,
        pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), TradePublication.class)
    );
  }

  @Override
  public boolean tryReserveSlots(String publicationId, int requested) {
    // Atomic: checks remainingCount - ifNull(pendingCount,0) >= requested and increments.
    // $ifNull handles legacy docs that don't have the pendingCount field.
    // $inc version forces OptimisticLockingFailureException on any concurrent save(),
    // ensuring acceptProposal reloads fresh data on its retry.
    Document filterDoc = new Document("_id", new ObjectId(publicationId))
        .append("status", "ACTIVE")
        .append("$expr", new Document("$gte", List.of(
            new Document("$subtract", List.of(
                "$remainingCount",
                new Document("$ifNull", List.of("$pendingCount", 0))
            )),
            requested
        )));
    Update update = new Update().inc("pendingCount", requested).inc("version", 1L);
    return mongoTemplate.findAndModify(new BasicQuery(filterDoc), update, TradePublication.class) != null;
  }

  @Override
  public void releaseSlots(String publicationId, int amount) {
    // Condition pendingCount >= amount prevents underflow on legacy docs without the field.
    // $inc version invalidates any concurrent save() of acceptProposal.
    Document filterDoc = new Document("_id", new ObjectId(publicationId))
        .append("pendingCount", new Document("$gte", amount));
    Update update = new Update().inc("pendingCount", -amount).inc("version", 1L);
    mongoTemplate.updateFirst(new BasicQuery(filterDoc), update, TradePublication.class);
  }
}
