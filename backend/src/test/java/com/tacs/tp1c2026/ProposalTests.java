package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

public class ProposalTests extends IntegrationTestBase {

  /** Crea publi de Alice ofreciendo `card_001` x{publishQty}. Devuelve publicationId. */
  private String publisherSetup(Session alice, int publishQty) throws Exception {
    addToCollectionN(alice.userId(), "card_001", publishQty, alice.token());
    return idFromCreated(publish(alice.token(), "card_001", publishQty), "publicationId");
  }

  /** Setup proposer Bob con 3 cards card_002 disponibles. */
  private Session proposerSetup() throws Exception {
    Session bob = register("Bob", "bob@test.com", "password123");
    addToCollectionN(bob.userId(), "card_002", 3, bob.token());
    return bob;
  }

  @Test
  void createProposalCommitsBidderCardsAndPersistsAsTopLevelDoc() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    String pubId = publisherSetup(alice, 2);
    Session bob = proposerSetup();

    MvcResult res = propose(bob.token(), pubId, List.of("card_002"), 1);
    String proposalId = idFromCreated(res, "proposalId");
    assertNotNull(proposalId);
    assertTrue(proposalId.length() > 0);
    assertEquals(1, proposalRepository.findByPublicationId(pubId).size());
  }

  @Test
  void cannotProposeOnOwnPublication() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    String pubId = publisherSetup(alice, 2);
    addToCollection(alice.userId(), "card_002", alice.token());

    String body = objectMapper.writeValueAsString(java.util.Map.of(
        "publicationId", pubId,
        "cardIds", List.of("card_002"),
        "requestedCount", 1
    ));
    mockMvc.perform(post("/api/proposals")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + alice.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void listSentReturnsProposalsCreatedByUser() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    String pubId = publisherSetup(alice, 2);
    Session bob = proposerSetup();
    propose(bob.token(), pubId, List.of("card_002"), 1);

    MvcResult res = mockMvc.perform(get("/api/proposals")
            .param("role", "proposer")
            .header("Authorization", "Bearer " + bob.token()))
        .andExpect(status().isOk())
        .andReturn();
    String body = res.getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$")).size());
    assertEquals(pubId, JsonPath.read(body, "$[0].publicationId"));
  }

  @Test
  void listReceivedReturnsProposalsOnUserPublications() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    String pubId = publisherSetup(alice, 2);
    Session bob = proposerSetup();
    propose(bob.token(), pubId, List.of("card_002"), 1);

    MvcResult res = mockMvc.perform(get("/api/proposals")
            .param("role", "publisher")
            .header("Authorization", "Bearer " + alice.token()))
        .andExpect(status().isOk())
        .andReturn();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(res.getResponse().getContentAsString(), "$")).size());
  }

  @Test
  void acceptProposalDecrementsRemainingAndTransfersCards() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    String pubId = publisherSetup(alice, 2);
    Session bob = proposerSetup();
    String proposalId = idFromCreated(propose(bob.token(), pubId, List.of("card_002"), 1), "proposalId");

    acceptProposal(alice.token(), proposalId);

    // Publi: remaining 1, status ACTIVE
    MvcResult pub = mockMvc.perform(get("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + alice.token())).andReturn();
    assertEquals(1, (Integer) JsonPath.read(pub.getResponse().getContentAsString(), "$.remainingCount"));
    assertEquals("ACTIVE", JsonPath.read(pub.getResponse().getContentAsString(), "$.status"));
  }

  @Test
  void acceptProposalThatExhaustsRemainingFinalizesAndCancelsPending() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    String pubId = publisherSetup(alice, 2);   // initial=2
    Session carol = register("Carol", "carol@test.com", "password123");
    addToCollection(carol.userId(), "card_003", carol.token());

    // Carol pide 1 primero. Después Bob pide 2 (entra porque pendingRequested=1 < remaining=2).
    // Al aceptar a Bob, remaining→0 ⇒ publi FINALIZED ⇒ Carol cancelada en cascada.
    String carolProposal = idFromCreated(propose(carol.token(), pubId, List.of("card_003"), 1), "proposalId");
    Session bob = proposerSetup();
    String bobProposal = idFromCreated(propose(bob.token(), pubId, List.of("card_002"), 2), "proposalId");

    acceptProposal(alice.token(), bobProposal);

    // Publi finalizada
    MvcResult pub = mockMvc.perform(get("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + alice.token())).andReturn();
    assertEquals("FINALIZED", JsonPath.read(pub.getResponse().getContentAsString(), "$.status"));

    // Carol's proposal cancelada en cascada
    MvcResult prop = mockMvc.perform(get("/api/proposals/" + carolProposal)
            .header("Authorization", "Bearer " + alice.token())).andReturn();
    assertEquals("CANCELLED", JsonPath.read(prop.getResponse().getContentAsString(), "$.status"));
  }

  @Test
  void rejectProposalReleasesProposerCommit() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    String pubId = publisherSetup(alice, 2);
    Session bob = proposerSetup();
    String proposalId = idFromCreated(propose(bob.token(), pubId, List.of("card_002"), 1), "proposalId");

    rejectProposal(alice.token(), proposalId);

    MvcResult col = mockMvc.perform(get("/api/users/" + bob.userId() + "/collection")
            .header("Authorization", "Bearer " + bob.token())).andReturn();
    String body = col.getResponse().getContentAsString();
    assertEquals(0, (Integer) JsonPath.read(body, "$[0].compromisedCount"));
  }

  @Test
  void cancelProposalByProposerReleasesCommit() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    String pubId = publisherSetup(alice, 2);
    Session bob = proposerSetup();
    String proposalId = idFromCreated(propose(bob.token(), pubId, List.of("card_002"), 1), "proposalId");

    cancelProposal(bob.token(), proposalId);

    MvcResult col = mockMvc.perform(get("/api/users/" + bob.userId() + "/collection")
            .header("Authorization", "Bearer " + bob.token())).andReturn();
    assertEquals(0, (Integer) JsonPath.read(col.getResponse().getContentAsString(), "$[0].compromisedCount"));
  }
}
