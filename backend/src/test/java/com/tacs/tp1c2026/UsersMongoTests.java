package com.tacs.tp1c2026;

import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests originales de la migración a MongoDB. Refactorizados para usar IntegrationTestBase
 * (limpieza de repos + helpers de auth) en lugar de armar el user directo por repository,
 * lo que dejaba al request sin JWT y devolvía 401.
 */
public class UsersMongoTests extends IntegrationTestBase {

  @Test
  void getUsuarioPorIdDevuelve200() throws Exception {
    Session s = register("Test User", "test@test.com", "password123");

    mockMvc.perform(get("/api/users/" + s.userId())
            .header("Authorization", "Bearer " + s.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test User"))
        .andExpect(jsonPath("$.email").value("test@test.com"));
  }

  @Test
  void getCollectionDeUsuarioNuevoEsVacia() throws Exception {
    Session s = register("Test User", "test@test.com", "password123");

    mockMvc.perform(get("/api/users/" + s.userId() + "/collection")
            .header("Authorization", "Bearer " + s.token()))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }
}
