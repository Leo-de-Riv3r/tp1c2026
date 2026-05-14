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

    @Test
    void getSuggestionsReturnsMatchingUsersAfterGeneration() throws Exception {
        Session sessionA = register("UserA", "usera@test.com", "pass123");
        Session sessionB = register("UserB", "userb@test.com", "pass123");

        addToCollection(sessionB.userId(), "card_001", sessionB.token());
        addToCollection(sessionB.userId(), "card_002", sessionB.token());

        addMissingCard(sessionA.userId(), "card_001", sessionA.token());
        addMissingCard(sessionA.userId(), "card_002", sessionA.token());

        User userA = userRepository.findById(sessionA.userId()).orElseThrow();
        User userB = userRepository.findById(sessionB.userId()).orElseThrow();

        ProfileGroup group = new ProfileGroup(profileProperties);
        group.addNeighbor(userA);
        group.addNeighbor(userB);
        profileGroupRepository.save(group);

        group.updateVector();
        profileGroupRepository.save(group);

        profileService.updateSuggestionsForUsers();

        String body = mockMvc.perform(get("/api/users/" + sessionA.userId() + "/suggestions")
                .header("Authorization", "Bearer " + sessionA.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        List<?> suggestions = JsonPath.read(body, "$");
        assertEquals(1, suggestions.size(), "should have 1 suggestion");
        assertEquals(sessionB.userId(), JsonPath.read(body, "$[0].suggestedUserId"));
        assertEquals("UserB", JsonPath.read(body, "$[0].suggestedUserName"));

        List<String> obtainableIds = JsonPath.read(body, "$[0].obtainableCards[*].cardId");
        assertEquals(2, obtainableIds.size());
    }

    @Test
    void getSuggestionsFiltersByCardsOtherUserActuallyHas() throws Exception {
        Session sessionA = register("UserA", "usera2@test.com", "pass123");
        Session sessionB = register("UserB", "userb2@test.com", "pass123");

        addToCollection(sessionB.userId(), "card_003", sessionB.token());

        addMissingCard(sessionA.userId(), "card_001", sessionA.token());
        addMissingCard(sessionA.userId(), "card_003", sessionA.token());

        User userA = userRepository.findById(sessionA.userId()).orElseThrow();
        User userB = userRepository.findById(sessionB.userId()).orElseThrow();

        ProfileGroup group = new ProfileGroup(profileProperties);
        group.addNeighbor(userA);
        group.addNeighbor(userB);
        profileGroupRepository.save(group);

        group.updateVector();
        profileGroupRepository.save(group);

        profileService.updateSuggestionsForUsers();

        String body = mockMvc.perform(get("/api/users/" + sessionA.userId() + "/suggestions")
                .header("Authorization", "Bearer " + sessionA.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        List<?> suggestions = JsonPath.read(body, "$");
        assertEquals(1, suggestions.size());

        List<String> obtainableIds = JsonPath.read(body, "$[0].obtainableCards[*].cardId");
        assertEquals(1, obtainableIds.size(), "only card_003 is obtainable");
        assertEquals("card_003", obtainableIds.get(0));
    }

    @Test
    void getSuggestionsIsEmptyForUserWithNoMissingCards() throws Exception {
        Session sessionA = register("UserA", "usera3@test.com", "pass123");
        Session sessionB = register("UserB", "userb3@test.com", "pass123");

        addToCollection(sessionB.userId(), "card_001", sessionB.token());

        addMissingCard(sessionA.userId(), "card_001", sessionA.token());

        User userA = userRepository.findById(sessionA.userId()).orElseThrow();
        User userB = userRepository.findById(sessionB.userId()).orElseThrow();

        ProfileGroup group = new ProfileGroup(profileProperties);
        group.addNeighbor(userA);
        group.addNeighbor(userB);
        profileGroupRepository.save(group);

        group.updateVector();
        profileGroupRepository.save(group);

        profileService.updateSuggestionsForUsers();

        String body = mockMvc.perform(get("/api/users/" + sessionB.userId() + "/suggestions")
                .header("Authorization", "Bearer " + sessionB.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        List<?> suggestions = JsonPath.read(body, "$");
        assertEquals(0, suggestions.size(), "user with no missing cards gets no suggestions");
    }

    @Test
    void getSuggestionsReturnsEmptyWhenNoOneHasMissingCard() throws Exception {
        Session sessionA = register("UserA", "usera4@test.com", "pass123");
        Session sessionB = register("UserB", "userb4@test.com", "pass123");

        addToCollection(sessionB.userId(), "card_003", sessionB.token());

        addMissingCard(sessionA.userId(), "card_099", sessionA.token());

        User userA = userRepository.findById(sessionA.userId()).orElseThrow();
        User userB = userRepository.findById(sessionB.userId()).orElseThrow();

        ProfileGroup group = new ProfileGroup(profileProperties);
        group.addNeighbor(userA);
        group.addNeighbor(userB);
        profileGroupRepository.save(group);

        group.updateVector();
        profileGroupRepository.save(group);

        profileService.updateSuggestionsForUsers();

        String body = mockMvc.perform(get("/api/users/" + sessionA.userId() + "/suggestions")
                .header("Authorization", "Bearer " + sessionA.token()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String suggestions = JsonPath.read(body, "$").toString();
        assertEquals("[]", suggestions, "no suggestions when no one has the missing card");
    }
}
