package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre el bloqueante "Cross-user authorization missing" de la devolución E3:
 *  - {@code UsersController} endpoints {@code /users/{id}/...} deben rechazar con 403 cuando el caller no es el dueño del recurso ni ADMIN.
 *  - El bypass para role ADMIN debe funcionar.
 *  - Los 401/403 emitidos por {@code JwtAuthenticationFilter}, {@code RoleInterceptor} y {@code OwnerOrAdminInterceptor} deben devolver body con shape {@code ApiError}.
 */
public class CrossUserAuthorizationTests extends IntegrationTestBase {

  private static final String CROSS_USER_MESSAGE = "No podés acceder a recursos de otro usuario";

  private void assertForbiddenCrossUserBody(MvcResult result) throws Exception {
    String body = result.getResponse().getContentAsString();
    assertEquals(403, (int) JsonPath.read(body, "$.status"));
    assertEquals("Forbidden", JsonPath.read(body, "$.error"));
    assertEquals(CROSS_USER_MESSAGE, JsonPath.read(body, "$.message"));
    assertNotNull(JsonPath.read(body, "$.timestamp"));
  }

  // ─────────────────── Cross-user → 403 por endpoint ───────────────────

  @Test
  void getByIdOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(get("/api/users/" + juan.userId())
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  @Test
  void getCollectionOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(get("/api/users/" + juan.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  @Test
  void addToCollectionOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(post("/api/users/" + juan.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"cardId\": \"FWC1\" }"))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  @Test
  void decrementFromCollectionOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(patch("/api/users/" + juan.userId() + "/collection/FWC1")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  @Test
  void getMissingCardsOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(get("/api/users/" + juan.userId() + "/missing-cards")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  @Test
  void addMissingCardOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(post("/api/users/" + juan.userId() + "/missing-cards")
            .header("Authorization", "Bearer " + pepe.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"cardId\": \"FWC1\" }"))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  @Test
  void removeMissingCardOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(delete("/api/users/" + juan.userId() + "/missing-cards/FWC1")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  @Test
  void getSuggestionsOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(get("/api/users/" + juan.userId() + "/suggestions")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  @Test
  void getNotificationsOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(get("/api/users/" + juan.userId() + "/notifications")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  @Test
  void markNotificationsAsReadOfOtherUserReturns403() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");
    Session juan = register("Juan", "juan@test.com", "password123");

    MvcResult res = mockMvc.perform(put("/api/users/" + juan.userId() + "/notifications/read")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isForbidden())
        .andReturn();
    assertForbiddenCrossUserBody(res);
  }

  // ─────────────────── Owner → 2xx (sanity) ───────────────────

  @Test
  void ownerCanReadOwnCollection() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");

    mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk());
  }

  // ─────────────────── ADMIN bypass ───────────────────

  @Test
  void adminCanReadOtherUserCollection() throws Exception {
    Session juan = register("Juan", "juan@test.com", "password123");
    Session admin = registerAdmin("Admin", "admin@test.com", "password123");

    mockMvc.perform(get("/api/users/" + juan.userId() + "/collection")
            .header("Authorization", "Bearer " + admin.token()))
        .andExpect(status().isOk());
  }

  @Test
  void adminCanModifyOtherUserCollection() throws Exception {
    Session juan = register("Juan", "juan@test.com", "password123");
    Session admin = registerAdmin("Admin", "admin@test.com", "password123");

    mockMvc.perform(post("/api/users/" + juan.userId() + "/collection")
            .header("Authorization", "Bearer " + admin.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"cardId\": \"FWC1\" }"))
        .andExpect(status().is2xxSuccessful());
  }

  // ─────────────── ApiError shape en filter / interceptors ───────────────

  @Test
  void missingTokenReturns401WithApiErrorBody() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection"))
        .andExpect(status().isUnauthorized())
        .andReturn().getResponse().getContentAsString();
    assertEquals(401, (int) JsonPath.read(body, "$.status"));
    assertEquals("Unauthorized", JsonPath.read(body, "$.error"));
    assertEquals("No se provee un token de autenticación", JsonPath.read(body, "$.message"));
    assertNotNull(JsonPath.read(body, "$.timestamp"));
  }

  @Test
  void invalidTokenReturns401WithApiErrorBody() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");

    String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/collection")
            .header("Authorization", "Bearer not-a-valid-jwt"))
        .andExpect(status().isUnauthorized())
        .andReturn().getResponse().getContentAsString();
    assertEquals(401, (int) JsonPath.read(body, "$.status"));
    assertEquals("Unauthorized", JsonPath.read(body, "$.error"));
    assertEquals("Sesión inválida o expirada", JsonPath.read(body, "$.message"));
    assertNotNull(JsonPath.read(body, "$.timestamp"));
  }

  @Test
  void nonAdminAccessingAdminOnlyEndpointReturns403WithApiErrorBody() throws Exception {
    Session pepe = register("Pepe", "pepe@test.com", "password123");

    String body = mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isForbidden())
        .andReturn().getResponse().getContentAsString();
    assertEquals(403, (int) JsonPath.read(body, "$.status"));
    assertEquals("Forbidden", JsonPath.read(body, "$.error"));
    assertEquals("No tenés permiso para acceder a este recurso", JsonPath.read(body, "$.message"));
    assertNotNull(JsonPath.read(body, "$.timestamp"));
  }
}
