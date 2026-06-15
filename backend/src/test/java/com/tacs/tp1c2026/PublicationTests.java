package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para PublicationsController. Convención de usuarios:
 *   Pepe (publicador) + Moni / Coqui / Dardo (proponentes).
 *
 * Organizado en cuatro secciones:
 *  1. Operaciones inválidas (validaciones, permisos, estado)
 *  2. Operaciones válidas (orden ascendente por cantidad)
 *  3. Estado y cascade (cancel libera, cancel cascade, búsqueda activa)
 *  4. Reads (lista propias, detalle)
 */
public class PublicationTests extends IntegrationTestBase {

  // ─────────────────────────── Operaciones inválidas ───────────────────────────

  @Test
  void publishWithoutCardInCollectionFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    String body = "{ \"cardId\": \"FWC1\", \"quantity\": 1 }";
    mockMvc.perform(post("/api/publications")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + pepe.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void publishZeroQuantityFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "FWC1", pepe.token());

    String body = "{ \"cardId\": \"FWC1\", \"quantity\": 0 }";
    mockMvc.perform(post("/api/publications")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + pepe.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void publishMoreThanAvailableFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "FWC1", 2, pepe.token());

    String body = "{ \"cardId\": \"FWC1\", \"quantity\": 3 }";
    mockMvc.perform(post("/api/publications")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + pepe.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void publishConsideringExistingCommitsFails() throws Exception {
    // Pepe tiene 3× FWC1: 1 ya comprometida en una subasta + 1 en una publi previa.
    // Available = 3 - 2 = 1. Si intenta publicar 2 más, falla.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "FWC1", 3, pepe.token());

    createAuction(pepe.token(), "FWC1", 24);
    publish(pepe.token(), "FWC1", 1);

    String body = "{ \"cardId\": \"FWC1\", \"quantity\": 2 }";
    mockMvc.perform(post("/api/publications")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + pepe.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void cancelPublicationByOtherUserFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "FWC1", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 1), "id");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    mockMvc.perform(delete("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + moni.token()))
        .andExpect(status().isForbidden());
  }

  @Test
  void cancelAlreadyCancelledPublicationFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "FWC1", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 1), "id");

    cancelPublication(pepe.token(), pubId);

    mockMvc.perform(delete("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void cancelFinalizedPublicationFails() throws Exception {
    // Pepe publica 1, Moni propone 1, Pepe acepta → publi FINALIZED. Cancelar debe fallar.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "FWC1", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 1), "id");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "FWC3", moni.token());
    String propId = idFromCreated(propose(moni.token(), pubId, List.of("FWC3"), 1), "proposalId");
    acceptProposal(pepe.token(), propId);

    mockMvc.perform(delete("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().is4xxClientError());
  }

  // ─────────────────── Operaciones válidas (orden ascendente) ───────────────────

  @Test
  void publishOneCardCommitsOne() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "FWC1", 2, pepe.token());

    MvcResult res = publish(pepe.token(), "FWC1", 1);
    String pubId = idFromCreated(res, "id");
    assertTrue(pubId.length() > 0);

    String coll = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(2, (Integer) JsonPath.read(coll, "$[0].quantity"));
    assertEquals(1, (Integer) JsonPath.read(coll, "$[0].compromisedCount"));

    String pub = mockMvc.perform(get("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, (Integer) JsonPath.read(pub, "$.initialCount"));
    assertEquals(1, (Integer) JsonPath.read(pub, "$.remainingCount"));
    assertEquals("ACTIVE", JsonPath.read(pub, "$.status"));
  }

  @Test
  void publishTwoCardsCommitsTwo() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "FWC1", 3, pepe.token());

    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 2), "id");

    String coll = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(3, (Integer) JsonPath.read(coll, "$[0].quantity"));
    assertEquals(2, (Integer) JsonPath.read(coll, "$[0].compromisedCount"));

    String pub = mockMvc.perform(get("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(2, (Integer) JsonPath.read(pub, "$.initialCount"));
    assertEquals(2, (Integer) JsonPath.read(pub, "$.remainingCount"));
  }

  // ─────────────────────────── Estado y cascade ───────────────────────────

  @Test
  void cancelPublicationReleasesCommittedCards() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "FWC1", 3, pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 2), "id");

    cancelPublication(pepe.token(), pubId);

    String coll = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(3, (Integer) JsonPath.read(coll, "$[0].quantity"));
    assertEquals(0, (Integer) JsonPath.read(coll, "$[0].compromisedCount"));
  }

  @Test
  void cancelPublicationCascadesToThreePendingProposals() throws Exception {
    // Pepe publica 3× FWC1. Moni / Coqui / Dardo proponen 1 card de la suya cada uno
    // (cada uno tiene 2 cards, así la diferencia compromised antes/después queda evidente).
    // Pepe cancela → 3 propuestas CANCELLED + commit liberado en cada proponente.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "FWC1", 3, pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 3), "id");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollectionN(moni.userId(), "FWC3", 2, moni.token());
    String moniProp = idFromCreated(propose(moni.token(), pubId, List.of("FWC3"), 1), "proposalId");

    Session coqui = register("Coqui Argento", "coquiargento@gmail.com", "password123");
    addToCollectionN(coqui.userId(), "ARG1", 2, coqui.token());
    String coquiProp = idFromCreated(propose(coqui.token(), pubId, List.of("ARG1"), 1), "proposalId");

    Session dardo = register("Dardo Fuseneco", "dardofuseneco@gmail.com", "password123");
    addToCollectionN(dardo.userId(), "BRA1", 2, dardo.token());
    String dardoProp = idFromCreated(propose(dardo.token(), pubId, List.of("BRA1"), 1), "proposalId");

    cancelPublication(pepe.token(), pubId);

    // Publicación cancelada
    String pub = mockMvc.perform(get("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals("CANCELLED", JsonPath.read(pub, "$.status"));

    // Las tres propuestas en CANCELLED
    for (String pid : List.of(moniProp, coquiProp, dardoProp)) {
      String p = mockMvc.perform(get("/api/proposals/" + pid)
              .header("Authorization", "Bearer " + pepe.token()))
          .andReturn().getResponse().getContentAsString();
      assertEquals("CANCELLED", JsonPath.read(p, "$.status"));
    }

    // Pepe: compromised liberado (3 → 0)
    assertCollection(pepe, 3, 0);
    // Cada proponente: compromised liberado (1 → 0), quantity sin cambios (2)
    assertCollection(moni, 2, 0);
    assertCollection(coqui, 2, 0);
    assertCollection(dardo, 2, 0);
  }

  @Test
  void cancelPublicationAfterPartialAcceptReleasesOnlyRemaining() throws Exception {
    // Pepe publica 3× FWC1 (commit=3, remaining=3). Moni propone+aceptada 1.
    // Tras el accept: Pepe transfirió 1 → quantity=2, compromised=2, remaining=2.
    // Pepe cancela → libera SOLO el remaining (2), no los 3 originales.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "FWC1", 3, pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 3), "id");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "FWC3", moni.token());
    String propId = idFromCreated(propose(moni.token(), pubId, List.of("FWC3"), 1), "proposalId");
    acceptProposal(pepe.token(), propId);

    cancelPublication(pepe.token(), pubId);

    // Pepe.FWC1: quantity 2 (transfirió 1, no se regenera al cancelar), compromised 0
    String coll = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    List<Integer> card001Qty = JsonPath.read(coll, "$[?(@.cardId=='FWC1')].quantity");
    List<Integer> card001Commit = JsonPath.read(coll, "$[?(@.cardId=='FWC1')].compromisedCount");
    assertEquals(2, (int) card001Qty.get(0));
    assertEquals(0, (int) card001Commit.get(0));

    String pub = mockMvc.perform(get("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals("CANCELLED", JsonPath.read(pub, "$.status"));
  }

  @Test
  void searchActivePublicationsLifecycle() throws Exception {
    // Publi activa aparece en el search; tras cancelar deja de aparecer.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "FWC1", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 1), "id");

    // El search activo excluye las publicaciones propias, así que busca otro user.
    Session searcher = register("Searcher", "searcher@gmail.com", "password123");

    List<String> activeIds = readActiveIds(searcher.token());
    assertTrue(activeIds.contains(pubId), "Una publi activa debería aparecer en el search");

    cancelPublication(pepe.token(), pubId);

    List<String> afterCancel = readActiveIds(searcher.token());
    assertFalse(afterCancel.contains(pubId), "Una publi cancelada no debería aparecer en el search activo");
  }

  // ─────────────────────────────── Reads ───────────────────────────────

  @Test
  void getMyPublicationsReturnsOwnList() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "FWC1", 2, pepe.token());
    addToCollectionN(pepe.userId(), "FWC3", 2, pepe.token());
    publish(pepe.token(), "FWC1", 1);
    publish(pepe.token(), "FWC3", 1);

    MvcResult res = mockMvc.perform(get("/api/publications")
            .param("userId", pepe.userId())
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn();
    String body = res.getResponse().getContentAsString();
    assertEquals(2, ((java.util.List<?>) JsonPath.read(body, "$.data")).size());
  }

  @Test
  void searchActivePublicationsFiltersByNameCountryAndCategory() throws Exception {
    // Pepe publica FWC1 ("Official Emblem", country="FIFA World Cup 2026", EPICO).
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "FWC1", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 1), "id");

    // El search activo excluye las publicaciones propias, así que busca otro user.
    Session searcher = register("Searcher", "searcher@gmail.com", "password123");

    // Filter by name (regex case-insensitive contra cardDescription)
    assertTrue(readActiveIds(searcher.token(), "name", "Emblem").contains(pubId));
    assertFalse(readActiveIds(searcher.token(), "name", "noexiste").contains(pubId));

    // Filter by category usando el value en español — verifica el fix del CategoryConverter.
    // Sin él, ?category=EPICO daba 400 y solo aceptaba ?category=EPIC.
    assertTrue(readActiveIds(searcher.token(), "category", "EPICO").contains(pubId));
    assertFalse(readActiveIds(searcher.token(), "category", "LEGENDARIO").contains(pubId));

    // Filter by country: FWC1 tiene country="FIFA World Cup 2026", así que "Argentina" no matchea
    assertFalse(readActiveIds(searcher.token(), "country", "Argentina").contains(pubId));
  }

  @Test
  void getPublicationByIdReturnsDetail() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "FWC1", 2, pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "FWC1", 1), "id");

    String body = mockMvc.perform(get("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(pubId, JsonPath.read(body, "$.id"));
    assertEquals(1, (Integer) JsonPath.read(body, "$.initialCount"));
    assertEquals(1, (Integer) JsonPath.read(body, "$.remainingCount"));
  }

  // ───────────────────────────── Helpers locales ─────────────────────────────

  /** Asserta quantity y compromisedCount del primer (y único) item de colección del user. */
  private void assertCollection(Session s, int expectedQuantity, int expectedCommitted) throws Exception {
    String body = mockMvc.perform(get("/api/users/" + s.userId() + "/collection")
            .header("Authorization", "Bearer " + s.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(expectedQuantity, (Integer) JsonPath.read(body, "$[0].quantity"),
        "quantity de " + s.userId());
    assertEquals(expectedCommitted, (Integer) JsonPath.read(body, "$[0].compromisedCount"),
        "compromisedCount de " + s.userId());
  }

  /** Lee la lista paginada de publicaciones activas y devuelve sólo sus IDs. */
  private List<String> readActiveIds(String token) throws Exception {
    return readActiveIds(token, null, null);
  }

  /** Variante con un único query param de filtro (name, country, team, category). */
  @SuppressWarnings("unchecked")
  private List<String> readActiveIds(String token, String paramName, String paramValue) throws Exception {
    var req = get("/api/publications").header("Authorization", "Bearer " + token);
    if (paramName != null) req = req.param(paramName, paramValue);
    String body = mockMvc.perform(req)
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return (List<String>) JsonPath.read(body, "$.data[*].id");
  }
}
