package com.tacs.tp1c2026.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tacs.tp1c2026.entities.match.UpcomingMatch;
import com.tacs.tp1c2026.entities.match.UpcomingMatchesCache;
import com.tacs.tp1c2026.repositories.UpcomingMatchesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bonus: integra football-data.org para traer los próximos partidos del Mundial.
 *
 * <ul>
 *   <li>{@link #refreshFromApi()} — lo llama el cron diario + el startup. Pega a
 *       {@code GET /competitions/{code}/matches?status=SCHEDULED,TIMED}, mapea, ordena por fecha y
 *       cachea los primeros {@code next}. Resiliente: si no hay key, si la API falla, o si devuelve
 *       0 partidos, conserva el cache previo (no lo pisa con vacío).</li>
 *   <li>{@link #getCachedMatches()} — lectura barata para el endpoint, lee sólo de Mongo.</li>
 * </ul>
 */
@Service
public class UpcomingMatchesService {

  private static final Logger log = LoggerFactory.getLogger(UpcomingMatchesService.class);

  // ObjectMapper propio (Jackson 2): parseamos el body como String para no depender del message
  // converter del framework (Spring Boot 4 usa Jackson 3 → no mapea el JsonNode de Jackson 2).
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final RestClient footballDataRestClient;
  private final UpcomingMatchesRepository repository;

  private final String apiKey;
  private final String competition;
  private final int next;

  public UpcomingMatchesService(RestClient footballDataRestClient,
                                UpcomingMatchesRepository repository,
                                @Value("${footballdata.key:}") String apiKey,
                                @Value("${footballdata.competition:WC}") String competition,
                                @Value("${footballdata.next:5}") int next) {
    this.footballDataRestClient = footballDataRestClient;
    this.repository = repository;
    this.apiKey = apiKey;
    this.competition = competition;
    this.next = next;
  }

  /** Devuelve lo cacheado (lista vacía si todavía no se pobló). */
  public List<UpcomingMatch> getCachedMatches() {
    return repository.findById(UpcomingMatchesCache.SINGLETON_ID)
        .map(UpcomingMatchesCache::getMatches)
        .orElseGet(List::of);
  }

  /** Refresca el cache desde football-data.org. No-op si falta la key; conserva el cache ante fallos. */
  public void refreshFromApi() {
    if (apiKey == null || apiKey.isBlank()) {
      log.warn("UpcomingMatchesService: FOOTBALLDATA_KEY sin configurar — se omite el refresh.");
      return;
    }

    JsonNode body;
    try {
      String raw = footballDataRestClient.get()
          .uri(uri -> uri.path("/competitions/{code}/matches")
              .queryParam("status", "SCHEDULED,TIMED")
              .build(competition))
          .retrieve()
          .body(String.class);
      body = (raw == null || raw.isBlank()) ? null : objectMapper.readTree(raw);
    } catch (Exception e) {
      log.error("UpcomingMatchesService: falló el GET a football-data.org: {}", e.getMessage());
      return;
    }

    List<UpcomingMatch> matches = parse(body);
    if (matches.isEmpty()) {
      log.warn("UpcomingMatchesService: football-data.org no devolvió partidos próximos (competition={}). "
          + "Se conserva el cache previo.", competition);
      return;
    }

    // El API ya los ordena por fecha; nos quedamos con los próximos `next`.
    List<UpcomingMatch> soonest = matches.stream().limit(next).toList();
    repository.save(new UpcomingMatchesCache(soonest, Instant.now()));
    log.info("UpcomingMatchesService: cache de próximos partidos actualizado ({} partidos).", soonest.size());
  }

  /** Mapea la respuesta de football-data.org ({@code {matches: [{utcDate, homeTeam, awayTeam, ...}]}}). */
  List<UpcomingMatch> parse(JsonNode body) {
    List<UpcomingMatch> matches = new ArrayList<>();
    if (body == null) {
      return matches;
    }
    JsonNode arr = body.path("matches");
    if (!arr.isArray()) {
      return matches;
    }
    for (JsonNode m : arr) {
      JsonNode home = m.path("homeTeam");
      JsonNode away = m.path("awayTeam");
      matches.add(new UpcomingMatch(
          textOrNull(home.path("name")),
          textOrNull(home.path("crest")),
          textOrNull(away.path("name")),
          textOrNull(away.path("crest")),
          toUtcIso(textOrNull(m.path("utcDate"))),
          textOrNull(m.path("venue")),
          round(m)
      ));
    }
    return matches;
  }

  /** Etapa del torneo: usa el grupo si está, si no la fase ({@code stage}). */
  private String round(JsonNode m) {
    String group = textOrNull(m.path("group"));
    return group != null ? group : textOrNull(m.path("stage"));
  }

  private String textOrNull(JsonNode node) {
    return node == null || node.isNull() || node.isMissingNode() ? null : node.asText(null);
  }

  /** Normaliza la fecha de la API (ISO con offset/Z) a ISO-8601 UTC. Fallback: el string original. */
  private String toUtcIso(String apiDate) {
    if (apiDate == null || apiDate.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(apiDate).toInstant().toString();
    } catch (Exception e) {
      return apiDate;
    }
  }
}
