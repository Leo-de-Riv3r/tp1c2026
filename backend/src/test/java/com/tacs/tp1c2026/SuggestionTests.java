package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.profiles.ProfileGroup;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.properties.ProfileProperties;
import com.tacs.tp1c2026.repositories.ProfileGroupRepository;
import com.tacs.tp1c2026.services.ProfileService;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite del nuevo modelo de Suggestion: el cron genera sugerencias apuntando a publications/auctions
 * ACTIVE concretas (no a usuarios pelados). Cada sugerencia tiene `sourceType` + `sourceId` que
 * referencia la entidad real
 */
public class SuggestionTests extends IntegrationTestBase {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileGroupRepository profileGroupRepository;

    @Autowired
    private ProfileProperties profileProperties;

    @BeforeEach
    void cleanProfileGroups() {
        profileGroupRepository.deleteAll();
    }

    /** Sin sugerencias precargadas, el endpoint devuelve []. */
    @Test
    void getSuggestionsReturnsEmptyListWhenNoneExist() throws Exception {
        Session pepe = register("Pepe", "pepe@test.com", "pass123");

        String body = mockMvc.perform(get("/api/users/" + pepe.userId() + "/suggestions")
                .header("Authorization", "Bearer " + pepe.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        List<?> suggestions = JsonPath.read(body, "$");
        assertEquals(0, suggestions.size());
    }

    /**
     * Si un candidato publicó una card que el current user busca, la sugerencia apunta a esa publicación.
     * sourceType=PUBLICATION, sourceId=id de la publicación, snapshots de card + publisher
     */
    @Test
    void publicationSuggestionIsGeneratedForMatchingCard() throws Exception {
        Session sessionA = register("UserA", "usera@test.com", "pass123");
        Session sessionB = register("UserB", "userb@test.com", "pass123");

        addToCollection(sessionB.userId(), "FWC1", sessionB.token());
        String pubId = idFromCreated(publish(sessionB.token(), "FWC1", 1), "id");

        addMissingCard(sessionA.userId(), "FWC1", sessionA.token());

        connectInProfileGroup(sessionA, sessionB);
        profileService.updateSuggestionsForUsers();

        String body = mockMvc.perform(get("/api/users/" + sessionA.userId() + "/suggestions")
                .header("Authorization", "Bearer " + sessionA.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertEquals(1, ((List<?>) JsonPath.read(body, "$")).size());
        assertEquals("PUBLICATION", JsonPath.read(body, "$[0].sourceType"));
        assertEquals(pubId,         JsonPath.read(body, "$[0].sourceId"));
        assertEquals("FWC1",    JsonPath.read(body, "$[0].cardId"));
        assertEquals("UserB",       JsonPath.read(body, "$[0].publisherName"));
        assertEquals(sessionB.userId(), JsonPath.read(body, "$[0].publisherUserId"));
    }

    /**
     * Si un candidato subastó una card que el current user busca, la sugerencia es de tipo AUCTION
     */
    @Test
    void auctionSuggestionIsGeneratedForMatchingCard() throws Exception {
        Session sessionA = register("UserA", "userauc@test.com", "pass123");
        Session sessionB = register("UserB", "userbauc@test.com", "pass123");

        addToCollection(sessionB.userId(), "MEX1", sessionB.token());
        String aucId = idFromCreated(createAuction(sessionB.token(), "MEX1", 24), "id");

        addMissingCard(sessionA.userId(), "MEX1", sessionA.token());

        connectInProfileGroup(sessionA, sessionB);
        profileService.updateSuggestionsForUsers();

        String body = mockMvc.perform(get("/api/users/" + sessionA.userId() + "/suggestions")
                .header("Authorization", "Bearer " + sessionA.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertEquals(1, ((List<?>) JsonPath.read(body, "$")).size());
        assertEquals("AUCTION",  JsonPath.read(body, "$[0].sourceType"));
        assertEquals(aucId,      JsonPath.read(body, "$[0].sourceId"));
        assertEquals("MEX1", JsonPath.read(body, "$[0].cardId"));
    }

    /**
     * Si un candidato tiene la card en su colección pero NO la publicó/subastó, no hay sugerencia
     * — solo se sugiere contenido público (publication/auction activa)
     */
    @Test
    void noSuggestionWhenCandidateHasCardInCollectionButDidNotPublish() throws Exception {
        Session sessionA = register("UserA", "userac@test.com", "pass123");
        Session sessionB = register("UserB", "userbc@test.com", "pass123");

        // UserB tiene la card pero no la publica ni subasta
        addToCollection(sessionB.userId(), "FWC1", sessionB.token());

        addMissingCard(sessionA.userId(), "FWC1", sessionA.token());

        connectInProfileGroup(sessionA, sessionB);
        profileService.updateSuggestionsForUsers();

        String body = mockMvc.perform(get("/api/users/" + sessionA.userId() + "/suggestions")
                .header("Authorization", "Bearer " + sessionA.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertEquals(0, ((List<?>) JsonPath.read(body, "$")).size());
    }

    /**
     * Solo las publications/auctions cuyas cards estén en `missingCards` del current user
     * se transforman en sugerencias. Las que no matchean se descartan
     */
    @Test
    void onlySuggestsPublicationsMatchingMissingCards() throws Exception {
        Session sessionA = register("UserA", "useraf@test.com", "pass123");
        Session sessionB = register("UserB", "userbf@test.com", "pass123");

        // UserB publica dos cards: FWC1 (la que UserA busca) y ARG1 (que no busca)
        addToCollection(sessionB.userId(), "FWC1", sessionB.token());
        addToCollection(sessionB.userId(), "ARG1", sessionB.token());
        String pubId001 = idFromCreated(publish(sessionB.token(), "FWC1", 1), "id");
        idFromCreated(publish(sessionB.token(), "ARG1", 1), "id");

        addMissingCard(sessionA.userId(), "FWC1", sessionA.token());

        connectInProfileGroup(sessionA, sessionB);
        profileService.updateSuggestionsForUsers();

        String body = mockMvc.perform(get("/api/users/" + sessionA.userId() + "/suggestions")
                .header("Authorization", "Bearer " + sessionA.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertEquals(1, ((List<?>) JsonPath.read(body, "$")).size());
        assertEquals(pubId001,   JsonPath.read(body, "$[0].sourceId"));
        assertEquals("FWC1", JsonPath.read(body, "$[0].cardId"));
    }

    /**
     * Si el user no tiene missing cards, no hay nada que sugerirle (early-return en el algoritmo)
     */
    @Test
    void getSuggestionsIsEmptyForUserWithNoMissingCards() throws Exception {
        Session sessionA = register("UserA", "userae@test.com", "pass123");
        Session sessionB = register("UserB", "userbe@test.com", "pass123");

        addToCollection(sessionB.userId(), "FWC1", sessionB.token());
        idFromCreated(publish(sessionB.token(), "FWC1", 1), "id");

        // UserA no tiene missing cards

        connectInProfileGroup(sessionA, sessionB);
        profileService.updateSuggestionsForUsers();

        String body = mockMvc.perform(get("/api/users/" + sessionA.userId() + "/suggestions")
                .header("Authorization", "Bearer " + sessionA.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertEquals(0, ((List<?>) JsonPath.read(body, "$")).size());
    }

    /**
     * Setup helper: pone a ambos users como vecinos de un ProfileGroup nuevo y refresca el vector
     * — necesario para que el cron los encuentre como candidatos entre sí
     */
    private void connectInProfileGroup(Session a, Session b) {
        User userA = userRepository.findById(a.userId()).orElseThrow();
        User userB = userRepository.findById(b.userId()).orElseThrow();

        ProfileGroup group = new ProfileGroup(profileProperties);
        group.addNeighbor(userA);
        group.addNeighbor(userB);
        profileGroupRepository.save(group);

        group.updateVector();
        profileGroupRepository.save(group);
    }
}
