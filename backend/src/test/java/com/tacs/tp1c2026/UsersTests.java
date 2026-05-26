package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.enums.UserRole;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para UsersController. Cubre:
 *  - Reads: getAll, getById, getMissingCards (getCollection ya está en UserFlowsTests)
 *  - Collection: POST (alta nueva / increment), PATCH (decrement)
 *  - Missing cards: POST (alta + idempotencia), DELETE
 *  - Invariantes: card desconocida, decrement sobre comprometida, missing sobre card en colección
 *
 * Nota: el flujo "ya la conseguí" (POST /collection limpia el missing matcheante) ya está
 * cubierto en UserFlowsTests.addToCollectionCleansMatchingMissingCard.
 */
public class UsersTests extends IntegrationTestBase {

  // ──────────────────────────── OK: Reads ────────────────────────────

  @Test
  void getAllUsersReturnsRegisteredList() throws Exception {
    register("Pepe Argento", "peperacing@gmail.com", "password123");
    register("Moni Argento", "moniargento@gmail.com", "password123");
    // GET /api/users es ADMIN-only. Promovemos a un user a admin y re-logueamos para JWT con role=ADMIN
    register("Admin", "admin@test.com", "password123");
    User adminUser = userRepository.findByEmail("admin@test.com").orElseThrow();
    adminUser.setRole(UserRole.ADMIN);
    userRepository.save(adminUser);
    Session admin = login("admin@test.com", "password123");

    String body = mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + admin.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    // El admin se filtra del response (UserService.getAll excluye admins)
    assertEquals(2, ((List<?>) JsonPath.read(body, "$")).size());
  }

  @Test
  void getUserByIdReturnsDto() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    String body = mockMvc.perform(get("/api/users/" + pepe.userId())
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(pepe.userId(), JsonPath.read(body, "$.id"));
    assertEquals("peperacing@gmail.com", JsonPath.read(body, "$.email"));
    assertEquals("Pepe Argento", JsonPath.read(body, "$.name"));
  }

  @Test
  void getMissingCardsReturnsListForUser() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addMissingCard(pepe.userId(), "card_001", pepe.token());
    addMissingCard(pepe.userId(), "card_002", pepe.token());

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/missing-cards")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(2, ((List<?>) JsonPath.read(body, "$")).size());
  }

  @Test
  void getUserByIdNotFoundReturns404() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    mockMvc.perform(get("/api/users/no-existe-este-id")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isNotFound());
  }

  // ──────────────────────── OK: Collection (POST) ────────────────────────

  @Test
  void addToCollectionCreatesNewEntryWhenAbsent() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    addToCollection(pepe.userId(), "card_001", pepe.token());

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(body, "$")).size());
    assertEquals("card_001", JsonPath.read(body, "$[0].cardId"));
    assertEquals(1, (Integer) JsonPath.read(body, "$[0].quantity"));
    assertEquals(0, (Integer) JsonPath.read(body, "$[0].compromisedCount"));
  }

  @Test
  void addToCollectionIncrementsQuantityWhenPresent() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    addToCollection(pepe.userId(), "card_001", pepe.token());
    addToCollection(pepe.userId(), "card_001", pepe.token()); // misma card → quantity++

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(body, "$")).size(), "no se duplican entries");
    assertEquals(2, (Integer) JsonPath.read(body, "$[0].quantity"));
  }

  // ──────────────────── OK: Collection PATCH (decrement) ────────────────────

  @Test
  void decrementFromCollectionReducesQuantity() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollectionN(pepe.userId(), "card_001", 3, pepe.token());

    mockMvc.perform(patch("/api/users/" + pepe.userId() + "/collection/card_001")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().is2xxSuccessful());

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(2, (Integer) JsonPath.read(body, "$[0].quantity"));
  }

  @Test
  void decrementFromCollectionRemovesEntryAtZero() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token()); // quantity=1

    mockMvc.perform(patch("/api/users/" + pepe.userId() + "/collection/card_001")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().is2xxSuccessful());

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(0, ((List<?>) JsonPath.read(body, "$")).size(), "la entry debe desaparecer al llegar a 0");
  }

  // ──────────────────────── OK: Missing cards ────────────────────────

  @Test
  void addMissingCardCreatesEntry() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    addMissingCard(pepe.userId(), "card_001", pepe.token());

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/missing-cards")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(body, "$")).size());
    assertEquals("card_001", JsonPath.read(body, "$[0].cardId"));
  }

  @Test
  void addMissingCardWhenAlreadyMissingIsIdempotent() throws Exception {
    // El service llama a User.addToMissingCards, que es idempotente: si ya existe, no agrega.
    // El endpoint devuelve 201 igual ambas veces, pero la lista no crece.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    addMissingCard(pepe.userId(), "card_001", pepe.token());
    addMissingCard(pepe.userId(), "card_001", pepe.token()); // segunda vez no duplica

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/missing-cards")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(body, "$")).size(), "no se duplican faltantes");
  }

  @Test
  void removeMissingCardDeletesEntry() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addMissingCard(pepe.userId(), "card_001", pepe.token());
    addMissingCard(pepe.userId(), "card_002", pepe.token());

    mockMvc.perform(delete("/api/users/" + pepe.userId() + "/missing-cards/card_001")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().is2xxSuccessful());

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/missing-cards")
            .header("Authorization", "Bearer " + pepe.token()))
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((List<?>) JsonPath.read(body, "$")).size());
    assertEquals("card_002", JsonPath.read(body, "$[0].cardId"));
  }

  // ──────────────────────────── Inválidos ────────────────────────────

  @Test
  void addToCollectionWithUnknownCardFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    String body = "{ \"cardId\": \"card_no_existe\" }";
    mockMvc.perform(post("/api/users/" + pepe.userId() + "/collection")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + pepe.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void decrementFromCollectionWhenNotInCollectionFails() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    // No agregamos card_001 a la colección. PATCH directo debería fallar.

    mockMvc.perform(patch("/api/users/" + pepe.userId() + "/collection/card_001")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void decrementFromCollectionForCommittedCardFails() throws Exception {
    // Pepe tiene 1× card_001, la publica (compromised=1, available=0). PATCH debe fallar
    // porque available < 1: si bajara la quantity, compromised quedaría > quantity, rompiendo el invariante.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    publish(pepe.token(), "card_001", 1);

    mockMvc.perform(patch("/api/users/" + pepe.userId() + "/collection/card_001")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void addMissingCardForCardAlreadyInCollectionFails() throws Exception {
    // El service tiene un guard explícito: si la card está en la colección, ConflictException.
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());

    String body = "{ \"cardId\": \"card_001\" }";
    mockMvc.perform(post("/api/users/" + pepe.userId() + "/missing-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + pepe.token())
            .content(body))
        .andExpect(status().is4xxClientError());
  }
}
