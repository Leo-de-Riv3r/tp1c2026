package com.tacs.tp1c2026;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Integración del endpoint del bonus. No hay FOOTBALLDATA_KEY en tests → el refresh es no-op y el
 * cache queda vacío, así que el endpoint devuelve {@code []} (200). Cubre el contrato (lectura del
 * cache, sin pegarle a la API) y que requiere auth.
 */
public class MatchesControllerTests extends IntegrationTestBase {

  @Test
  void upcomingReturns200AndJsonArrayForAuthedUser() throws Exception {
    Session user = register("matches_qa", "matches_qa@java.com", "password123");
    mockMvc.perform(get("/api/matches/upcoming")
            .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void upcomingRequiresAuth() throws Exception {
    mockMvc.perform(get("/api/matches/upcoming"))
        .andExpect(status().is4xxClientError());
  }
}
