package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

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
  void acceptedProposalTransfersBilaterally() throws Exception {
    // Pepe publica 2× card_001. Moni propone 1× card_002. Pepe acepta.
    //   - Publi: initial=2 (igual), remaining=1, status=ACTIVE
    //   - Pepe.card_001: quantity 2 → 1 (transfirió 1), compromised 2 → 1
    //   - Pepe.card_002: nueva entrada con quantity=1 (recibida)
    //   - Moni.card_002: quantity 3 → 2 (transfirió 1)
    //   - Moni.card_001: nueva entrada con quantity=1 (recibida)
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "card_001", 2, pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 2), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollectionN(moni.userId(), "card_002", 3, moni.token());
    String proposalId = idFromCreated(propose(moni.token(), pubId, List.of("card_002"), 1), "proposalId");

    acceptProposal(pepe.token(), proposalId);

    // Publi: initial sin cambios, remaining decrementado, sigue ACTIVE
    String pub = mockMvc.perform(get("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(2, (Integer) JsonPath.read(pub, "$.initialCount"));
    assertEquals(1, (Integer) JsonPath.read(pub, "$.remainingCount"));
    assertEquals("ACTIVE", JsonPath.read(pub, "$.status"));

    // Pepe: card_001 quantity-1 + compromised-1; card_002 recibida con quantity=1
    String pepeColl = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    List<Integer> pepe001Qty = JsonPath.read(pepeColl, "$[?(@.cardId=='card_001')].quantity");
    List<Integer> pepe001Commit = JsonPath.read(pepeColl, "$[?(@.cardId=='card_001')].compromisedCount");
    List<Integer> pepe002Qty = JsonPath.read(pepeColl, "$[?(@.cardId=='card_002')].quantity");
    assertEquals(1, (int) pepe001Qty.get(0));
    assertEquals(1, (int) pepe001Commit.get(0));
    assertEquals(1, (int) pepe002Qty.get(0));

    // Moni: card_002 quantity-1 (transferida); card_001 recibida con quantity=1
    String moniColl = mockMvc.perform(get("/api/users/" + moni.userId() + "/collection")
            .header("Authorization", "Bearer " + moni.token()))
        .andReturn().getResponse().getContentAsString();
    List<Integer> moni002Qty = JsonPath.read(moniColl, "$[?(@.cardId=='card_002')].quantity");
    List<Integer> moni001Qty = JsonPath.read(moniColl, "$[?(@.cardId=='card_001')].quantity");
    assertEquals(2, (int) moni002Qty.get(0));
    assertEquals(1, (int) moni001Qty.get(0));
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

  // ───────────────── OK: composición y commit de figuritas ofrecidas ─────────────────

  @Test
  void proposeOneCardCommitsOne() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "card_001", 2, pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 2), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollectionN(moni.userId(), "card_002", 2, moni.token());
    String proposalId = idFromCreated(propose(moni.token(), pubId, List.of("card_002"), 1), "proposalId");

    String moniColl = mockMvc.perform(get("/api/users/" + moni.userId() + "/collection")
            .header("Authorization", "Bearer " + moni.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(2, (Integer) JsonPath.read(moniColl, "$[0].quantity"));
    assertEquals(1, (Integer) JsonPath.read(moniColl, "$[0].compromisedCount"));

    String prop = mockMvc.perform(get("/api/proposals/" + proposalId)
            .header("Authorization", "Bearer " + moni.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(prop, "$.cardIds")).size());
  }

  @Test
  void proposeTwoDifferentCardsCommitsBoth() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    addToCollection(moni.userId(), "card_003", moni.token());
    propose(moni.token(), pubId, List.of("card_002", "card_003"), 1);

    String moniColl = mockMvc.perform(get("/api/users/" + moni.userId() + "/collection")
            .header("Authorization", "Bearer " + moni.token()))
        .andReturn().getResponse().getContentAsString();
    List<Integer> c002 = JsonPath.read(moniColl, "$[?(@.cardId=='card_002')].compromisedCount");
    List<Integer> c003 = JsonPath.read(moniColl, "$[?(@.cardId=='card_003')].compromisedCount");
    assertEquals(1, (int) c002.get(0));
    assertEquals(1, (int) c003.get(0));
  }

  @Test
  void proposeSameCardTwiceCommitsTwo() throws Exception {
    // Repetir el mismo cardId en cardIds equivale a ofrecer N unidades de esa figurita.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollectionN(moni.userId(), "card_002", 2, moni.token());
    propose(moni.token(), pubId, List.of("card_002", "card_002"), 1);

    String moniColl = mockMvc.perform(get("/api/users/" + moni.userId() + "/collection")
            .header("Authorization", "Bearer " + moni.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(2, (Integer) JsonPath.read(moniColl, "$[0].quantity"));
    assertEquals(2, (Integer) JsonPath.read(moniColl, "$[0].compromisedCount"));
  }

  // ─────────────── OK: cancelación con compromisos previos ───────────────

  @Test
  void cancelProposalReleasesOnlyOwnCommitsKeepingPriorOnes() throws Exception {
    // Moni ya tiene 1× card_002 comprometida en una subasta (compromised=1).
    // Después arma una propuesta con ["card_002", "card_003"] → card_002 compromised=2, card_003=1.
    // Al cancelar la propuesta, libera 1 de cada → card_002 compromised=1 (queda la subasta),
    // card_003 compromised=0. Más realista que cancelar y quedar todo en 0.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollectionN(moni.userId(), "card_002", 3, moni.token());
    addToCollection(moni.userId(), "card_003", moni.token());

    createAuction(moni.token(), "card_002", 24);
    String proposalId = idFromCreated(
        propose(moni.token(), pubId, List.of("card_002", "card_003"), 1), "proposalId");

    cancelProposal(moni.token(), proposalId);

    String moniColl = mockMvc.perform(get("/api/users/" + moni.userId() + "/collection")
            .header("Authorization", "Bearer " + moni.token()))
        .andReturn().getResponse().getContentAsString();
    List<Integer> c002Commit = JsonPath.read(moniColl, "$[?(@.cardId=='card_002')].compromisedCount");
    List<Integer> c003Commit = JsonPath.read(moniColl, "$[?(@.cardId=='card_003')].compromisedCount");
    assertEquals(1, (int) c002Commit.get(0), "card_002 mantiene el commit de la subasta");
    assertEquals(0, (int) c003Commit.get(0), "card_003 queda completamente libre");
  }

  // ───────────────── Inválidos: estado de la publicación ─────────────────

  @Test
  void proposeOnFinalizedPublicationFails() throws Exception {
    // Pepe publica 1 → Moni propone → Pepe acepta → publi FINALIZED.
    // Coqui intenta proponer y debe fallar.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    String moniProp = idFromCreated(propose(moni.token(), pubId, List.of("card_002"), 1), "proposalId");
    acceptProposal(pepe.token(), moniProp);

    Session coqui = register("Coqui Argento", "coquiargento@gmail.com", "password123");
    addToCollection(coqui.userId(), "card_003", coqui.token());
    String body = objectMapper.writeValueAsString(Map.of(
        "publicationId", pubId,
        "cardIds", List.of("card_003"),
        "requestedCount", 1));
    mockMvc.perform(post("/api/proposals")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + coqui.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void proposeOnCancelledPublicationFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");
    cancelPublication(pepe.token(), pubId);

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    String body = objectMapper.writeValueAsString(Map.of(
        "publicationId", pubId,
        "cardIds", List.of("card_002"),
        "requestedCount", 1));
    mockMvc.perform(post("/api/proposals")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + moni.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  // ───────────────── Inválidos: figuritas del proponente ─────────────────

  @Test
  void proposeCardNotInOwnCollectionFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    // Moni NO tiene card_002 en su colección
    String body = objectMapper.writeValueAsString(Map.of(
        "publicationId", pubId,
        "cardIds", List.of("card_002"),
        "requestedCount", 1));
    mockMvc.perform(post("/api/proposals")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + moni.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void proposeMoreThanAvailableQuantityFails() throws Exception {
    // Moni tiene 1× card_002, propone ["card_002", "card_002"] → segundo commit falla.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    String body = objectMapper.writeValueAsString(Map.of(
        "publicationId", pubId,
        "cardIds", List.of("card_002", "card_002"),
        "requestedCount", 1));
    mockMvc.perform(post("/api/proposals")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + moni.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  // ─────────────────────── Reads (detalle / filtros) ───────────────────────

  @Test
  void getProposalByIdReturnsDetail() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    String proposalId = idFromCreated(propose(moni.token(), pubId, List.of("card_002"), 1), "proposalId");

    String body = mockMvc.perform(get("/api/proposals/" + proposalId)
            .header("Authorization", "Bearer " + moni.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(proposalId, JsonPath.read(body, "$.id"));
    assertEquals(pubId, JsonPath.read(body, "$.publicationId"));
    assertEquals("PENDING", JsonPath.read(body, "$.status"));
  }

  @Test
  void getProposalByIdNotFoundReturns404() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    mockMvc.perform(get("/api/proposals/no-existe-este-id")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isNotFound());
  }

  @Test
  void listProposalsFilteredByStatus() throws Exception {
    // Pepe publica 2 unidades de card_001. Moni propone (PENDING) y Coqui propone+cancela
    // (CANCELLED). Filter por status devuelve solo las matcheantes.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "card_001", 2, pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 2), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    propose(moni.token(), pubId, List.of("card_002"), 1);

    Session coqui = register("Coqui Argento", "coquiargento@gmail.com", "password123");
    addToCollection(coqui.userId(), "card_003", coqui.token());
    String coquiProp = idFromCreated(propose(coqui.token(), pubId, List.of("card_003"), 1), "proposalId");
    cancelProposal(coqui.token(), coquiProp);

    // Pepe (publisher) ve sus propuestas recibidas filtradas
    String pending = mockMvc.perform(get("/api/proposals")
            .param("role", "publisher")
            .param("status", "PENDING")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(pending, "$")).size());

    String cancelled = mockMvc.perform(get("/api/proposals")
            .param("role", "publisher")
            .param("status", "CANCELLED")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(cancelled, "$")).size());
  }

  @Test
  void listProposalsFilteredByPublicationId() throws Exception {
    // Pepe arma 2 publis distintas. Moni propone en una sola.
    // Filter por publicationId devuelve solo las de esa publi.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    addToCollection(pepe.userId(), "card_005", pepe.token());
    String pubA = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");
    String pubB = idFromCreated(publish(pepe.token(), "card_005", 1), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    propose(moni.token(), pubA, List.of("card_002"), 1);

    String forA = mockMvc.perform(get("/api/proposals")
            .param("publicationId", pubA)
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(forA, "$")).size());

    String forB = mockMvc.perform(get("/api/proposals")
            .param("publicationId", pubB)
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(0, ((List<?>) JsonPath.read(forB, "$")).size());
  }

  @Test
  void proposeEmptyCardListFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    String pubId = idFromCreated(publish(pepe.token(), "card_001", 1), "publicationId");

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    String body = objectMapper.writeValueAsString(Map.of(
        "publicationId", pubId,
        "cardIds", List.of(),
        "requestedCount", 1));
    mockMvc.perform(post("/api/proposals")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + moni.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }
}
