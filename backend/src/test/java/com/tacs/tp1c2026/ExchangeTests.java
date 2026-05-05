package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

public class ExchangeTests extends IntegrationTestBase {

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
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$.figuritasDeA")).size());
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$.figuritasDeB")).size());
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
}
