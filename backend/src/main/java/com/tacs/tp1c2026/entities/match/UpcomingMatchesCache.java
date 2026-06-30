package com.tacs.tp1c2026.entities.match;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cache de los próximos partidos del Mundial (bonus). Documento ÚNICO con id fijo: el refresh
 * diario hace {@code save()} sobre el mismo id → upsert atómico, sin ventana de lista vacía y sin
 * depender de un índice TTL (que igual no se auto-crearía: {@code auto-index-creation=false}).
 */
@Document(collection = "upcoming_matches")
@TypeAlias("upcoming_matches_cache")
@Getter
@Setter
public class UpcomingMatchesCache {

  public static final String SINGLETON_ID = "current";

  @Id
  private String id = SINGLETON_ID;

  private List<UpcomingMatch> matches = new ArrayList<>();

  private Instant fetchedAt;

  public UpcomingMatchesCache() {
    // Constructor sin args para Spring Data Mongo.
  }

  public UpcomingMatchesCache(List<UpcomingMatch> matches, Instant fetchedAt) {
    this.id = SINGLETON_ID;
    this.matches = matches;
    this.fetchedAt = fetchedAt;
  }
}
