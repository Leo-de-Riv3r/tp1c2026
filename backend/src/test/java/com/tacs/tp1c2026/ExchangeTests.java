package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.services.AuctionService;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

public class ExchangeTests extends IntegrationTestBase {

  @Autowired
  private AuctionService auctionService;

  /** Setup: Alice publica card_001x1, Bob propone card_002, Alice acepta → 1 Exchange. */
  private record Setup(Session alice, Session bob, String exchangeId) {}

  private Setup setupAcceptedExchange() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    addToCollection(alice.userId(), "card_001", alice.token());
    String pubId = idFromCreated(publish(alice.token(), "card_001", 1), "publicationId");

    Session bob = register("Bob", "bob@test.com", "password123");
    addToCollection(bob.userId(), "card_002", bob.token());
    String proposalId = idFromCreated(propose(bob.token(), pubId, List.of("card_002"), 1), "proposalId");

    acceptProposal(alice.token(), proposalId);

    // El exchange se creó internamente; lo recupero por la lista
    MvcResult res = mockMvc.perform(get("/api/exchanges")
            .header("Authorization", "Bearer " + alice.token()))
        .andExpect(status().isOk())
        .andReturn();
    String exchangeId = JsonPath.read(res.getResponse().getContentAsString(), "$.data[0].id");
    return new Setup(alice, bob, exchangeId);
  }

  @Test
  void acceptedProposalCreatesHistoricExchangeForBothUsers() throws Exception {
    Setup s = setupAcceptedExchange();
    assertNotNull(s.exchangeId());

    MvcResult bobView = mockMvc.perform(get("/api/exchanges")
            .header("Authorization", "Bearer " + s.bob().token()))
        .andExpect(status().isOk())
        .andReturn();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(bobView.getResponse().getContentAsString(), "$.data")).size());
  }

  @Test
  void getExchangeByIdReturnsBilateralSnapshot() throws Exception {
    Setup s = setupAcceptedExchange();

    MvcResult res = mockMvc.perform(get("/api/exchanges/" + s.exchangeId())
            .header("Authorization", "Bearer " + s.alice().token()))
        .andExpect(status().isOk())
        .andReturn();
    String body = res.getResponse().getContentAsString();
    assertEquals(s.alice().userId(), JsonPath.read(body, "$.userA.userId"));
    assertEquals(s.bob().userId(), JsonPath.read(body, "$.userB.userId"));
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$.cardsFromA")).size());
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$.cardsFromB")).size());
  }

  @Test
  void addFeedbackPersistsInUserSlot() throws Exception {
    Setup s = setupAcceptedExchange();
    String body = "{ \"score\": 5, \"comment\": \"Genial\" }";

    mockMvc.perform(post("/api/exchanges/" + s.exchangeId() + "/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + s.alice().token())
            .content(body))
        .andExpect(status().isCreated());

    MvcResult res = mockMvc.perform(get("/api/exchanges/" + s.exchangeId())
            .header("Authorization", "Bearer " + s.alice().token()))
        .andReturn();
    String exchange = res.getResponse().getContentAsString();
    assertEquals(5, (Integer) JsonPath.read(exchange, "$.feedbackFromA.score"));
  }

  @Test
  void acceptedExchangeIncrementsExchangesAmountForBothUsers() throws Exception {
    Setup s = setupAcceptedExchange();

    MvcResult aliceRes = mockMvc.perform(get("/api/users/" + s.alice().userId())
            .header("Authorization", "Bearer " + s.alice().token())).andReturn();
    MvcResult bobRes = mockMvc.perform(get("/api/users/" + s.bob().userId())
            .header("Authorization", "Bearer " + s.bob().token())).andReturn();
    assertEquals(1, (Integer) JsonPath.read(aliceRes.getResponse().getContentAsString(), "$.exchangesAmount"));
    assertEquals(1, (Integer) JsonPath.read(bobRes.getResponse().getContentAsString(), "$.exchangesAmount"));
  }

  // ───────────────── OK: exchange originado por subasta ─────────────────

  @Test
  void acceptedBidCreatesHistoricExchangeWithAuctionOrigin() throws Exception {
    // Pepe subasta 1× card_001, Moni oferta 1× card_002, Pepe marca best y cierra → Exchange (SUBASTA).
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());

    String auctionId = setupAcceptedAuction(pepe, moni, "card_001", "card_002");

    // Pepe debería ver un exchange con origin SUBASTA
    String body = mockMvc.perform(get("/api/exchanges")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(body, "$.data")).size());
    String exchangeId = JsonPath.read(body, "$.data[0].id");

    String detail = mockMvc.perform(get("/api/exchanges/" + exchangeId)
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals("SUBASTA", JsonPath.read(detail, "$.origin.type"));
    assertEquals(auctionId, JsonPath.read(detail, "$.origin.id"));
    assertEquals(pepe.userId(), JsonPath.read(detail, "$.userA.userId"));
    assertEquals(moni.userId(), JsonPath.read(detail, "$.userB.userId"));
  }

  @Test
  void getExchangesReturnsMultipleEntriesForUser() throws Exception {
    // Pepe gana 2 exchanges: uno via propuesta (con Moni) + uno via subasta (con Coqui).
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "card_001", 2, pepe.token());

    // Exchange 1: proposal
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");
    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    String propId = idFromCreated(propose(moni.token(), pubId, List.of("card_002"), 1), "proposalId");
    acceptProposal(pepe.token(), propId);

    // Exchange 2: subasta (con la card que le queda)
    Session coqui = register("Coqui Argento", "coquiargento@gmail.com", "password123");
    addToCollection(coqui.userId(), "card_003", coqui.token());
    setupAcceptedAuction(pepe, coqui, "card_001", "card_003");

    String body = mockMvc.perform(get("/api/exchanges")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(2, ((List<?>) JsonPath.read(body, "$.data")).size());
  }

  // ───────────────── OK: feedback de ambos lados ─────────────────

  @Test
  void feedbackFromBothUsersPersistsInTheirSlots() throws Exception {
    Setup s = setupAcceptedExchange();
    String feedback = "{ \"score\": 4, \"comment\": \"Bien\" }";

    // Alice deja feedback → feedbackFromA
    mockMvc.perform(post("/api/exchanges/" + s.exchangeId() + "/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + s.alice().token())
            .content(feedback))
        .andExpect(status().isCreated());
    // Bob deja feedback → feedbackFromB
    mockMvc.perform(post("/api/exchanges/" + s.exchangeId() + "/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + s.bob().token())
            .content("{ \"score\": 5, \"comment\": \"Ok\" }"))
        .andExpect(status().isCreated());

    String body = mockMvc.perform(get("/api/exchanges/" + s.exchangeId())
            .header("Authorization", "Bearer " + s.alice().token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(4, (Integer) JsonPath.read(body, "$.feedbackFromA.score"));
    assertEquals(5, (Integer) JsonPath.read(body, "$.feedbackFromB.score"));
  }

  @Test
  void addFeedbackRecalculatesReviewedUserRating() throws Exception {
    Setup s = setupAcceptedExchange();

    // Alice (A) deja feedback score=4 sobre Bob (B) → bob.rating debería ser 4.0
    mockMvc.perform(post("/api/exchanges/" + s.exchangeId() + "/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + s.alice().token())
            .content("{ \"score\": 4, \"comment\": \"Bien\" }"))
        .andExpect(status().isCreated());

    String bobBody = mockMvc.perform(get("/api/users/" + s.bob().userId())
            .header("Authorization", "Bearer " + s.bob().token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(4.0, ((Number) JsonPath.read(bobBody, "$.rating")).doubleValue(), 0.001);

    // Bob (B) deja feedback score=5 sobre Alice (A) → alice.rating debería ser 5.0
    mockMvc.perform(post("/api/exchanges/" + s.exchangeId() + "/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + s.bob().token())
            .content("{ \"score\": 5, \"comment\": \"Genial\" }"))
        .andExpect(status().isCreated());

    String aliceBody = mockMvc.perform(get("/api/users/" + s.alice().userId())
            .header("Authorization", "Bearer " + s.alice().token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(5.0, ((Number) JsonPath.read(aliceBody, "$.rating")).doubleValue(), 0.001);
  }

  // ───────────────── Inválidos: permisos ─────────────────

  @Test
  void cannotListAnotherUsersExchanges() throws Exception {
    Setup s = setupAcceptedExchange();
    Session coqui = register("Coqui Argento", "coquiargento@gmail.com", "password123");

    mockMvc.perform(get("/api/exchanges")
            .param("userId", s.alice().userId())
            .header("Authorization", "Bearer " + coqui.token()))
        .andExpect(status().isForbidden());
  }

  @Test
  void cannotGetExchangeAsNonParticipant() throws Exception {
    Setup s = setupAcceptedExchange();
    Session coqui = register("Coqui Argento", "coquiargento@gmail.com", "password123");

    mockMvc.perform(get("/api/exchanges/" + s.exchangeId())
            .header("Authorization", "Bearer " + coqui.token()))
        .andExpect(status().isForbidden());
  }

  @Test
  void getExchangeByIdNotFoundReturns404() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    mockMvc.perform(get("/api/exchanges/no-existe-este-id")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isNotFound());
  }

  // ───────────────── Inválidos: feedback ─────────────────

  @Test
  void feedbackFromNonParticipantFails() throws Exception {
    Setup s = setupAcceptedExchange();
    Session coqui = register("Coqui Argento", "coquiargento@gmail.com", "password123");

    mockMvc.perform(post("/api/exchanges/" + s.exchangeId() + "/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + coqui.token())
            .content("{ \"score\": 5, \"comment\": \"Hola\" }"))
        .andExpect(status().isForbidden());
  }

  @Test
  void feedbackFromSameUserTwiceFails() throws Exception {
    Setup s = setupAcceptedExchange();
    String body = "{ \"score\": 5, \"comment\": \"OK\" }";

    // Primero OK
    mockMvc.perform(post("/api/exchanges/" + s.exchangeId() + "/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + s.alice().token())
            .content(body))
        .andExpect(status().isCreated());
    // Segundo intento del mismo usuario → 4xx (ConflictException → 409)
    mockMvc.perform(post("/api/exchanges/" + s.exchangeId() + "/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + s.alice().token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void feedbackWithScoreOutOfRangeFails() throws Exception {
    Setup s = setupAcceptedExchange();

    mockMvc.perform(post("/api/exchanges/" + s.exchangeId() + "/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + s.alice().token())
            .content("{ \"score\": 10, \"comment\": \"Fuera de rango\" }"))
        .andExpect(status().is4xxClientError());
  }

  // ───────────────────── Helpers locales ─────────────────────

  /**
   * Arma el flujo completo de una subasta adjudicada: crea subasta, oferta, marca best y cierra.
   * Devuelve el auctionId. Pre-condición: publisher tiene publishedCardId, winner tiene offeredCardId.
   */
  private String setupAcceptedAuction(Session publisher, Session winner,
                                       String publishedCardId, String offeredCardId) throws Exception {
    String auctionId = JsonPath.read(
        createAuction(publisher.token(), publishedCardId, 24).getResponse().getContentAsString(),
        "$.data.id");

    String offerBody = "{ \"auctionId\": \"" + auctionId + "\", \"items\": [{ \"cardId\": \""
        + offeredCardId + "\", \"amount\": 1 }] }";
    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + winner.token())
            .content(offerBody))
        .andExpect(status().is2xxSuccessful());

    String aucBody = mockMvc.perform(get("/api/auctions/" + auctionId)
            .header("Authorization", "Bearer " + publisher.token()))
        .andReturn().getResponse().getContentAsString();
    String offerId = JsonPath.read(aucBody, "$.offers[0].offerId");

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/best")
            .header("Authorization", "Bearer " + publisher.token()))
        .andExpect(status().is2xxSuccessful());

    // Cierra la subasta vía service (simula el job programado). El service:
    // - acepta la best offer, transfiere las cards, rechaza las demás
    // - crea el Exchange con origin SUBASTA (esto es lo que estamos testeando)
    auctionService.closeExpiredAuction(auctionId);
    return auctionId;
  }
}
