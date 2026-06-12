package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserFlowsTests extends IntegrationTestBase {

  @Test
  void registerCreatesUserAndReturnsToken() throws Exception {
    Session s = register("Alice", "alice@test.com", "password123");
    assertTrue(s.token().length() > 0);
    assertEquals(1, userRepository.count());
  }

  @Test
  void addToCollectionCleansMatchingMissingCard() throws Exception {
    Session s = register("Alice", "alice@test.com", "password123");
    addMissingCard(s.userId(), "FWC1", s.token());
    addMissingCard(s.userId(), "FWC3", s.token());

    addToCollection(s.userId(), "FWC1", s.token());

    // Sólo FWC1 sale de missing; FWC3 queda.
    MvcResult missingRes = mockMvc.perform(get("/api/users/" + s.userId() + "/missing-cards")
            .header("Authorization", "Bearer " + s.token()))
        .andExpect(status().isOk())
        .andReturn();
    String body = missingRes.getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$")).size());
    assertEquals("FWC3", JsonPath.read(body, "$[0].cardId"));
  }

  @Test
  void getCollectionReflectsAddedCards() throws Exception {
    Session s = register("Alice", "alice@test.com", "password123");
    addToCollectionN(s.userId(), "FWC1", 3, s.token());

    MvcResult res = mockMvc.perform(get("/api/users/" + s.userId() + "/collection")
            .header("Authorization", "Bearer " + s.token()))
        .andExpect(status().isOk())
        .andReturn();
    String body = res.getResponse().getContentAsString();
    assertEquals(3, (Integer) JsonPath.read(body, "$[0].quantity"));
    assertEquals(0, (Integer) JsonPath.read(body, "$[0].compromisedCount"));
  }
}
