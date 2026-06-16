package com.tacs.tp1c2026;

import com.tacs.tp1c2026.services.SettingsService;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Límites del modelo marketplace de propuestas: tope de propuestas PENDIENTES por publicación
 * (configurable por el admin), una sola propuesta activa por user sobre la misma publicación,
 * y sobre-suscripción permitida (varias pueden pedir el total disponible).
 */
class ProposalLimitsTests extends IntegrationTestBase {

  @Autowired
  private SettingsService settingsService;

  private ResultActions proposeRaw(String token, String pubId, List<String> cardIds, int requested) throws Exception {
    String body = objectMapper.writeValueAsString(Map.of(
        "publicationId", pubId, "cardIds", cardIds, "requestedCount", requested));
    return mockMvc.perform(post("/api/proposals")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .content(body));
  }

  @Test
  void segundaPropuestaPendienteDelMismoUserFalla() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    addToCollectionN(alice.userId(), "FWC1", 2, alice.token());
    String pubId = idFromCreated(publish(alice.token(), "FWC1", 2), "id");

    Session bob = register("Bob", "bob@test.com", "password123");
    addToCollectionN(bob.userId(), "MEX1", 2, bob.token());

    proposeRaw(bob.token(), pubId, List.of("MEX1"), 1).andExpect(status().is2xxSuccessful());
    // La 2da propuesta del mismo user sobre la misma publi se rechaza (UX-3), aunque tenga card libre.
    proposeRaw(bob.token(), pubId, List.of("MEX1"), 1).andExpect(status().isConflict());
  }

  @Test
  void propuestaSuperandoElTopeDePendientesFalla() throws Exception {
    settingsService.setMaxPendingProposals(1); // tope = 1 pendiente

    Session alice = register("Alice", "alice@test.com", "password123");
    addToCollectionN(alice.userId(), "FWC1", 2, alice.token());
    String pubId = idFromCreated(publish(alice.token(), "FWC1", 2), "id");

    Session bob = register("Bob", "bob@test.com", "password123");
    addToCollection(bob.userId(), "MEX1", bob.token());
    Session carol = register("Carol", "carol@test.com", "password123");
    addToCollection(carol.userId(), "ARG1", carol.token());

    proposeRaw(bob.token(), pubId, List.of("MEX1"), 1).andExpect(status().is2xxSuccessful());
    // Con el tope en 1, la 2da pendiente (de otro user) se rechaza con 409.
    proposeRaw(carol.token(), pubId, List.of("ARG1"), 1).andExpect(status().isConflict());
  }

  @Test
  void sobreSuscripcionPermitida() throws Exception {
    Session alice = register("Alice", "alice@test.com", "password123");
    addToCollectionN(alice.userId(), "FWC1", 2, alice.token());
    String pubId = idFromCreated(publish(alice.token(), "FWC1", 2), "id");

    Session bob = register("Bob", "bob@test.com", "password123");
    addToCollection(bob.userId(), "MEX1", bob.token());
    Session carol = register("Carol", "carol@test.com", "password123");
    addToCollection(carol.userId(), "ARG1", carol.token());

    // Ambos piden las 2 (= remaining). El agregado (4) supera lo disponible (2), pero se permite:
    // la escasez se resuelve al aceptar, no al crear.
    proposeRaw(bob.token(), pubId, List.of("MEX1"), 2).andExpect(status().is2xxSuccessful());
    proposeRaw(carol.token(), pubId, List.of("ARG1"), 2).andExpect(status().is2xxSuccessful());
  }
}
