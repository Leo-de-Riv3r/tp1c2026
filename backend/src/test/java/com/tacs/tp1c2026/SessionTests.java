package com.tacs.tp1c2026;

import com.tacs.tp1c2026.repositories.SessionRepository;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mecánica de las sesiones server-side (token opaco = sessionId). Cubre creación al loguear,
 * validación (válida / inválida / expirada), sliding al usar y revocación en el logout.
 */
class SessionTests extends IntegrationTestBase {

  @Autowired
  private SessionRepository sessionRepository;

  /** Pega a un endpoint autenticado del propio user y verifica el status esperado. */
  private void authedRequest(Session s, int expectedStatus) throws Exception {
    mockMvc.perform(get("/api/users/" + s.userId() + "/collection")
            .header("Authorization", "Bearer " + s.token()))
        .andExpect(status().is(expectedStatus));
  }

  @Test
  void loginCreaUnaSesionEnMongo() throws Exception {
    Session s = register("Pepe", "pepe@test.com", "password123");
    assertTrue(sessionRepository.findById(s.token()).isPresent(),
        "el login debería persistir una sesión con el token como id");
  }

  @Test
  void sesionValidaPermiteRequestAutenticado() throws Exception {
    Session s = register("Pepe", "pepe@test.com", "password123");
    authedRequest(s, 200);
  }

  @Test
  void tokenInvalidoDa401() throws Exception {
    Session s = register("Pepe", "pepe@test.com", "password123");
    mockMvc.perform(get("/api/users/" + s.userId() + "/collection")
            .header("Authorization", "Bearer no-es-una-sesion-real"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutRevocaLaSesion() throws Exception {
    Session s = register("Pepe", "pepe@test.com", "password123");
    authedRequest(s, 200); // antes del logout: ok

    mockMvc.perform(post("/api/auth/logout")
            .header("Authorization", "Bearer " + s.token()))
        .andExpect(status().isNoContent());

    assertTrue(sessionRepository.findById(s.token()).isEmpty(), "el logout debería borrar el doc");
    authedRequest(s, 401); // token revocado
  }

  @Test
  void sesionExpiradaDa401YSeLimpia() throws Exception {
    Session s = register("Pepe", "pepe@test.com", "password123");
    var doc = sessionRepository.findById(s.token()).orElseThrow();
    doc.setExpiresAt(Instant.now().minusSeconds(60)); // vencida
    sessionRepository.save(doc);

    authedRequest(s, 401);
    assertTrue(sessionRepository.findById(s.token()).isEmpty(),
        "una sesión vencida debería borrarse al validar");
  }

  @Test
  void usarLaSesionEstiraElVencimiento() throws Exception {
    Session s = register("Pepe", "pepe@test.com", "password123");
    var doc = sessionRepository.findById(s.token()).orElseThrow();
    doc.setExpiresAt(Instant.now().plusSeconds(30)); // vencimiento chico pero válido
    sessionRepository.save(doc);

    authedRequest(s, 200); // al usarla, se "toca" (sliding)

    Instant after = sessionRepository.findById(s.token()).orElseThrow().getExpiresAt();
    // El TTL default es 1h, así que tras tocar debe quedar mucho más adelante que los 30s seteados.
    assertTrue(after.isAfter(Instant.now().plusSeconds(1800)),
        "el expiresAt debería re-extenderse al TTL completo (~1h) al usar la sesión");
  }
}
