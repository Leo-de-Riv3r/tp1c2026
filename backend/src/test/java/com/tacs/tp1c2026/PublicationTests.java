package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

public class PublicationTests extends IntegrationTestBase {

  @Test
  void createPublicationCommitsCardsAndReturnsId() throws Exception {
    Session s = register("Alice", "alice@test.com", "password123");
    addToCollectionN(s.userId(), "card_001", 3, s.token());

    MvcResult res = publish(s.token(), "card_001", 2);
    String pubId = idFromCreated(res, "publicationId");
    assertTrue(pubId.length() > 0);

    // compromisedCount queda en 2, available = 1
    MvcResult col = mockMvc.perform(get("/api/users/" + s.userId() + "/collection")
            .header("Authorization", "Bearer " + s.token()))
        .andReturn();
    String body = col.getResponse().getContentAsString();
    assertEquals(2, (Integer) JsonPath.read(body, "$[0].compromisedCount"));
  }

  @Test
  void publishWithoutCardInCollectionFails() throws Exception {
    Session s = register("Alice", "alice@test.com", "password123");
    String body = "{ \"cardId\": \"card_001\", \"quantity\": 1 }";
    mockMvc.perform(post("/api/publications")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + s.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void getMyPublicationsReturnsOwnList() throws Exception {
    Session s = register("Alice", "alice@test.com", "password123");
    addToCollectionN(s.userId(), "card_001", 2, s.token());
    addToCollectionN(s.userId(), "card_002", 2, s.token());
    publish(s.token(), "card_001", 1);
    publish(s.token(), "card_002", 1);

    MvcResult res = mockMvc.perform(get("/api/publications")
            .param("userId", s.userId())
            .header("Authorization", "Bearer " + s.token()))
        .andExpect(status().isOk())
        .andReturn();
    String body = res.getResponse().getContentAsString();
    assertEquals(2, ((java.util.List<?>) JsonPath.read(body, "$.data")).size());
  }

  @Test
  void cancelPublicationReleasesCommittedCards() throws Exception {
    Session s = register("Alice", "alice@test.com", "password123");
    addToCollectionN(s.userId(), "card_001", 3, s.token());
    String pubId = idFromCreated(publish(s.token(), "card_001", 2), "publicationId");

    cancelPublication(s.token(), pubId);

    MvcResult col = mockMvc.perform(get("/api/users/" + s.userId() + "/collection")
            .header("Authorization", "Bearer " + s.token()))
        .andReturn();
    assertEquals(0, (Integer) JsonPath.read(col.getResponse().getContentAsString(), "$[0].compromisedCount"));
  }

  @Test
  void getPublicationByIdReturnsDetail() throws Exception {
    Session s = register("Alice", "alice@test.com", "password123");
    addToCollectionN(s.userId(), "card_001", 2, s.token());
    String pubId = idFromCreated(publish(s.token(), "card_001", 1), "publicationId");

    MvcResult res = mockMvc.perform(get("/api/publications/" + pubId)
            .header("Authorization", "Bearer " + s.token()))
        .andExpect(status().isOk())
        .andReturn();
    String body = res.getResponse().getContentAsString();
    assertEquals(pubId, JsonPath.read(body, "$.publicationId"));
    assertEquals(1, (Integer) JsonPath.read(body, "$.initialCount"));
    assertEquals(1, (Integer) JsonPath.read(body, "$.remainingCount"));
  }
}
