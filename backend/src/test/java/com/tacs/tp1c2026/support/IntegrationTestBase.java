package com.tacs.tp1c2026.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.UserRole;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.CardRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.repositories.ProposalRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.repositories.ScheduledUserNotificationsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de integración: limpia repos, carga catálogo y expone helpers para registrar /
 * loguear usuarios y armar requests autenticados. Cada subclase test usa MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestAsyncConfig.class, TestcontainersMongoConfig.class})
public abstract class IntegrationTestBase {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected CardRepository cardRepository;
  @Autowired protected UserRepository userRepository;
  @Autowired protected AuctionRepository auctionRepository;
  @Autowired protected PublicationRepository publicationRepository;
  @Autowired protected ProposalRepository proposalRepository;
  @Autowired protected ScheduledUserNotificationsRepository scheduledUserNotificationsRepository;
  @Autowired protected MongoTemplate mongoTemplate;

  protected final ObjectMapper objectMapper = new ObjectMapper();

  /** Wrapper simple del par token + userId del usuario logueado. */
  public static record Session(String token, String userId) {}

  private static boolean seeded = false;

  @BeforeEach
  void cleanAndSeed() throws Exception {
    userRepository.deleteAll();
    auctionRepository.deleteAll();
    publicationRepository.deleteAll();
    scheduledUserNotificationsRepository.deleteAll();
    mongoTemplate.dropCollection("proposals");
    mongoTemplate.dropCollection("exchanges");

    if (!seeded) {
      try (InputStream stream = getClass().getResourceAsStream("/catalog.json")) {
        if (stream != null) {
          List<Card> cards = objectMapper.readValue(stream, new TypeReference<List<Card>>() {});
          cardRepository.saveAll(cards);
        }
      }
      seeded = true;
    }
  }

  protected Session register(String name, String email, String password) throws Exception {
    String body = "{ \"name\": \"" + name + "\", \"email\": \"" + email
        + "\", \"password\": \"" + password + "\", \"avatarId\": \"avatar_1\" }";
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().is2xxSuccessful());
    return login(email, password);
  }

  /**
   * Crea un user con role ADMIN directamente en BD (sin pasar por /api/auth/register, que solo emite USER)
   * y devuelve la session ya logueada. El JWT trae el claim {@code role=ADMIN}, lo que activa el bypass de {@link com.tacs.tp1c2026.config.OwnerOrAdminInterceptor} en endpoints de otros users
   */
  protected Session registerAdmin(String name, String email, String password) throws Exception {
    User admin = new User();
    admin.setName(name);
    admin.setEmail(email.toLowerCase());
    admin.setAvatarId("avatar_1");
    admin.setPasswordHash(new BCryptPasswordEncoder().encode(password));
    admin.setRole(UserRole.ADMIN);
    userRepository.save(admin);
    return login(email, password);
  }

  protected Session login(String email, String password) throws Exception {
    String body = "{ \"email\": \"" + email + "\", \"password\": \"" + password + "\" }";
    MvcResult res = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andReturn();
    String response = res.getResponse().getContentAsString();
    String token = JsonPath.read(response, "$.token");
    String userId = JsonPath.read(response, "$.user.id");
    return new Session(token, userId);
  }

  protected void addToCollection(String userId, String cardId, String token) throws Exception {
    String body = "{ \"cardId\": \"" + cardId + "\" }";
    mockMvc.perform(post("/api/users/" + userId + "/collection")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(body))
        .andExpect(status().is2xxSuccessful());
  }

  protected void addToCollectionN(String userId, String cardId, int times, String token) throws Exception {
    for (int i = 0; i < times; i++) addToCollection(userId, cardId, token);
  }

  protected void addMissingCard(String userId, String cardId, String token) throws Exception {
    String body = "{ \"cardId\": \"" + cardId + "\" }";
    mockMvc.perform(post("/api/users/" + userId + "/missing-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(body))
        .andExpect(status().is2xxSuccessful());
  }

  protected MvcResult publish(String token, String cardId, int quantity) throws Exception {
    String body = "{ \"cardId\": \"" + cardId + "\", \"quantity\": " + quantity + " }";
    return mockMvc.perform(post("/api/publications")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(body))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
  }

  protected MvcResult propose(String token, String publicationId, List<String> cardIds, int requestedCount) throws Exception {
    String body = objectMapper.writeValueAsString(Map.of(
        "publicationId", publicationId,
        "cardIds", cardIds,
        "requestedCount", requestedCount
    ));
    return mockMvc.perform(post("/api/proposals")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(body))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
  }

  protected void acceptProposal(String token, String proposalId) throws Exception {
    mockMvc.perform(put("/api/proposals/" + proposalId + "/accept")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().is2xxSuccessful());
  }

  protected void rejectProposal(String token, String proposalId) throws Exception {
    mockMvc.perform(put("/api/proposals/" + proposalId + "/reject")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().is2xxSuccessful());
  }

  protected void cancelProposal(String token, String proposalId) throws Exception {
    mockMvc.perform(put("/api/proposals/" + proposalId + "/cancel")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().is2xxSuccessful());
  }

  protected void cancelPublication(String token, String publicationId) throws Exception {
    mockMvc.perform(delete("/api/publications/" + publicationId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().is2xxSuccessful());
  }

  protected MvcResult createAuction(String token, String cardId, int durationHours) throws Exception {
    String body = "{ \"cardId\": \"" + cardId + "\", \"auctionDurationHours\": " + durationHours
        + ", \"conditions\": [] }";
    return mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(body))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
  }

  /**
   * Extrae el id del recurso recién creado del body de un POST envuelto en
   * {@code CreatedResponseDto<T>}. Los DTOs no son consistentes en cómo nombran el id
   * ({@code AuctionDto} y {@code TradeProposalDto} usan {@code id};
   * {@code TradePublicationDto} usa {@code publicationId}; {@code AuctionOfferDto} usa
   * {@code offerId}). El helper intenta {@code $.data.id} primero, sino cae al
   * {@code key} pasado por el caller. Cuando se uniformicen los DTOs a {@code id},
   * el parámetro {@code key} puede eliminarse
   */
  protected String idFromCreated(MvcResult result, String key) throws Exception {
    String body = result.getResponse().getContentAsString();
    try {
      return JsonPath.read(body, "$.data.id");
    } catch (PathNotFoundException e) {
      return JsonPath.read(body, "$.data." + key);
    }
  }
}
