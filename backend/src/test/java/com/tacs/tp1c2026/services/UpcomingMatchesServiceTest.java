package com.tacs.tp1c2026.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tacs.tp1c2026.entities.match.UpcomingMatch;
import com.tacs.tp1c2026.entities.match.UpcomingMatchesCache;
import com.tacs.tp1c2026.repositories.UpcomingMatchesRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests del bonus (sin contexto Spring / Mongo / Docker): cubren el mapeo de la respuesta de
 * football-data.org, la lectura del cache, el guard de "sin API key", y el camino HTTP completo
 * (fetch → parse → recorte a {@code next} → save) vía {@link MockRestServiceServer}. Vive en el
 * package del service para poder ejercitar el {@code parse(...)} package-private sin exponerlo.
 */
class UpcomingMatchesServiceTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private UpcomingMatchesService serviceWith(UpcomingMatchesRepository repo) {
    // RestClient null: estos tests no ejercitan el camino que lo usa (parse/getCached/no-op key).
    return new UpcomingMatchesService(null, repo, "", "WC", 5);
  }

  // ─── parse(...) ────────────────────────────────────────────────────────────

  @Test
  void parseMapsMatchFieldsAndNormalizesKickoffToUtc() throws Exception {
    String json = """
        { "matches": [
          { "utcDate": "2026-06-11T16:00:00-03:00",
            "stage": "GROUP_STAGE", "group": "Group A", "venue": "Estadio Azteca",
            "homeTeam": { "name": "Mexico", "crest": "https://x/mex.png" },
            "awayTeam": { "name": "Canada", "crest": "https://x/can.png" } }
        ] }
        """;
    JsonNode node = mapper.readTree(json);

    List<UpcomingMatch> matches = serviceWith(mock(UpcomingMatchesRepository.class)).parse(node);

    assertEquals(1, matches.size());
    UpcomingMatch m = matches.get(0);
    assertEquals("Mexico", m.getHomeTeam());
    assertEquals("https://x/mex.png", m.getHomeCrest());
    assertEquals("Canada", m.getAwayTeam());
    assertEquals("https://x/can.png", m.getAwayCrest());
    assertEquals("Estadio Azteca", m.getVenue());
    assertEquals("Group A", m.getRound()); // usa el grupo si está
    assertEquals("2026-06-11T19:00:00Z", m.getKickoff()); // -03:00 → UTC
  }

  @Test
  void parseFallsBackToStageWhenNoGroup() throws Exception {
    String json = """
        { "matches": [
          { "utcDate": "2026-07-19T19:00:00Z", "stage": "FINAL",
            "homeTeam": { "name": "A" }, "awayTeam": { "name": "B" } }
        ] }
        """;
    List<UpcomingMatch> matches = serviceWith(mock(UpcomingMatchesRepository.class)).parse(mapper.readTree(json));
    assertEquals("FINAL", matches.get(0).getRound());
  }

  @Test
  void parseHandlesNullAndEmptyResponses() throws Exception {
    UpcomingMatchesService svc = serviceWith(mock(UpcomingMatchesRepository.class));
    assertTrue(svc.parse(null).isEmpty());
    assertTrue(svc.parse(mapper.readTree("{}")).isEmpty());
    assertTrue(svc.parse(mapper.readTree("{ \"matches\": [] }")).isEmpty());
  }

  @Test
  void parseLeavesMissingFieldsAsNull() throws Exception {
    JsonNode node = mapper.readTree("{ \"matches\": [ { \"homeTeam\": {}, \"awayTeam\": {} } ] }");
    List<UpcomingMatch> matches = serviceWith(mock(UpcomingMatchesRepository.class)).parse(node);
    assertEquals(1, matches.size());
    assertNull(matches.get(0).getHomeTeam());
    assertNull(matches.get(0).getKickoff());
  }

  // ─── getCachedMatches() ──────────────────────────────────────────────────────

  @Test
  void getCachedMatchesReadsFromRepository() {
    UpcomingMatchesRepository repo = mock(UpcomingMatchesRepository.class);
    UpcomingMatch m = new UpcomingMatch("A", null, "B", null, "2026-06-11T19:00:00Z", null, null);
    when(repo.findById(UpcomingMatchesCache.SINGLETON_ID))
        .thenReturn(Optional.of(new UpcomingMatchesCache(List.of(m), Instant.now())));

    List<UpcomingMatch> result = serviceWith(repo).getCachedMatches();

    assertEquals(1, result.size());
    assertEquals("A", result.get(0).getHomeTeam());
  }

  @Test
  void getCachedMatchesEmptyWhenNothingCached() {
    UpcomingMatchesRepository repo = mock(UpcomingMatchesRepository.class);
    when(repo.findById(UpcomingMatchesCache.SINGLETON_ID)).thenReturn(Optional.empty());
    assertTrue(serviceWith(repo).getCachedMatches().isEmpty());
  }

  // ─── refreshFromApi() ────────────────────────────────────────────────────────

  @Test
  void refreshIsNoOpWithoutApiKey() {
    UpcomingMatchesRepository repo = mock(UpcomingMatchesRepository.class);
    serviceWith(repo).refreshFromApi(); // apiKey vacía → no toca el repo ni la API
    verifyNoInteractions(repo);
  }

  @Test
  void refreshFetchesParsesAndSavesOnlyNextMatches() {
    // La API devuelve 7 partidos; con next=5 deben guardarse sólo los 5 primeros.
    String items = IntStream.rangeClosed(1, 7)
        .mapToObj(i -> "{\"utcDate\":\"2026-07-0" + i + "T19:00:00Z\",\"stage\":\"LAST_16\","
            + "\"homeTeam\":{\"name\":\"H" + i + "\"},\"awayTeam\":{\"name\":\"A" + i + "\"}}")
        .collect(Collectors.joining(","));
    String json = "{\"matches\":[" + items + "]}";

    RestClient.Builder builder = RestClient.builder().baseUrl("https://fd.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(containsString("/competitions/WC/matches")))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    UpcomingMatchesRepository repo = mock(UpcomingMatchesRepository.class);
    UpcomingMatchesService svc = new UpcomingMatchesService(builder.build(), repo, "fake-key", "WC", 5);

    svc.refreshFromApi();

    server.verify();
    ArgumentCaptor<UpcomingMatchesCache> captor = ArgumentCaptor.forClass(UpcomingMatchesCache.class);
    verify(repo).save(captor.capture());
    List<UpcomingMatch> saved = captor.getValue().getMatches();
    assertEquals(5, saved.size());
    assertEquals("H1", saved.get(0).getHomeTeam());
    assertEquals("H5", saved.get(4).getHomeTeam());
  }

  @Test
  void refreshKeepsCacheWhenApiReturnsNoMatches() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://fd.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(containsString("/competitions/WC/matches")))
        .andRespond(withSuccess("{\"matches\":[]}", MediaType.APPLICATION_JSON));

    UpcomingMatchesRepository repo = mock(UpcomingMatchesRepository.class);
    new UpcomingMatchesService(builder.build(), repo, "fake-key", "WC", 5).refreshFromApi();

    server.verify();
    verify(repo, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void refreshKeepsCacheOnHttpError() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://fd.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(containsString("/competitions/WC/matches")))
        .andRespond(withServerError());

    UpcomingMatchesRepository repo = mock(UpcomingMatchesRepository.class);
    new UpcomingMatchesService(builder.build(), repo, "fake-key", "WC", 5).refreshFromApi();

    verify(repo, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
