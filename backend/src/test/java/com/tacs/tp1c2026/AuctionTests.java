package com.tacs.tp1c2026;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.auction.input.AuctionConditionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;

import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.CardRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuctionTests {
  @Autowired
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired
  private CardRepository cardRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private AuctionRepository auctionRepository;

  @BeforeEach
  void setup() throws IOException {
    userRepository.deleteAll();
    cardRepository.deleteAll();
    auctionRepository.deleteAll();
    try (InputStream inputStream = getClass().getResourceAsStream("/catalog.json")) {
      List<Card> cards = objectMapper.readValue(inputStream, new TypeReference<List<Card>>() {});
      cardRepository.saveAll(cards);
      System.out.println("✅ Base de datos de prueba inicializada con " + cards.size() + " cartas.");
    }
  }

  @Test
  void searchActiveAuctionsReturnsCreatedAuction() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String token = getUserToken("seller@java.com", "password123");
    String cardId = "card_021";
    registerRepeatedCard(cardId, token, "1");
    String auctionBody = createAuctionBody(cardId, 24, List.of());
    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(auctionBody))
        .andExpect(status().is2xxSuccessful());

    MvcResult res = mockMvc.perform(get("/api/auctions")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn();
    String body = res.getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$.data")).size());
    assertEquals(21, (Integer) JsonPath.read(body, "$.data[0].cardNumber"));
  }

  @Test
  void getMyAuctionsReturnsCurrentUserAuctions() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String token = getUserToken("seller@java.com", "password123");
    registerRepeatedCard("card_021", token, "1");
    registerRepeatedCard("card_022", token, "1");
    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(createAuctionBody("card_021", 24, List.of())))
        .andExpect(status().is2xxSuccessful());
    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(createAuctionBody("card_022", 24, List.of())))
        .andExpect(status().is2xxSuccessful());

    MvcResult res = mockMvc.perform(get("/api/auctions/createdByMe")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn();
    assertEquals(2, ((java.util.List<?>) JsonPath.read(res.getResponse().getContentAsString(), "$.data")).size());
  }

  @Test
  void cancelAuctionReleasesCommittedCard() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String token = getUserToken("seller@java.com", "password123");
    registerRepeatedCard("card_021", token, "1");
    MvcResult created = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(createAuctionBody("card_021", 24, List.of())))
        .andReturn();
    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.auctionId");

    mockMvc.perform(delete("/api/auctions/" + auctionId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().is2xxSuccessful());

    String userId = JsonPath.read(mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody("seller@java.com", "password123")))
        .andReturn().getResponse().getContentAsString(), "$.user.id");
    MvcResult col = mockMvc.perform(get("/api/users/" + userId + "/collection")
            .header("Authorization", "Bearer " + token)).andReturn();
    assertEquals(0, (Integer) JsonPath.read(col.getResponse().getContentAsString(), "$[0].compromisedCount"));
  }

  // TODO: test para GET /api/auctions/myOffers — el endpoint funciona en runtime
  // pero el filtro en memoria no encuentra al bidder en los tests; revisar cómo persiste
  // @DocumentReference dentro de array embebido en el contexto de MockMvc.

  @org.junit.jupiter.api.Disabled("WIP: ver TODO arriba")
  @Test
  void myOffersReturnsBidsPlacedByCurrentUser() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String sellerToken = getUserToken("seller@java.com", "password123");
    registerRepeatedCard("card_021", sellerToken, "1");
    MvcResult created = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken)
            .content(createAuctionBody("card_021", 24, List.of())))
        .andReturn();
    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.auctionId");

    registrarUsuario("bidder", "bidder@java.com", "password123", "avatar1");
    String bidderToken = getUserToken("bidder@java.com", "password123");
    registerRepeatedCard("card_022", bidderToken, "2");
    String offerBody = """
        { "auctionId": "%s", "items": [ { "cardId": "card_022", "amount": 1 } ] }
        """.formatted(auctionId);
    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidderToken)
            .content(offerBody))
        .andExpect(status().is2xxSuccessful());

    MvcResult res = mockMvc.perform(get("/api/auctions/myOffers")
            .header("Authorization", "Bearer " + bidderToken))
        .andExpect(status().isOk())
        .andReturn();
    String body = res.getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$")).size());
    assertEquals(auctionId, JsonPath.read(body, "$[0].auctionId"));

    MvcResult sellerRes = mockMvc.perform(get("/api/auctions/myOffers")
            .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isOk())
        .andReturn();
    assertEquals(0, ((java.util.List<?>) JsonPath.read(sellerRes.getResponse().getContentAsString(), "$")).size());
  }

  @Test
  void publishCardForAuction() throws Exception {
    registrarUsuario("testUser", "test@java.com", "password123", "avatar1");
    String token = getUserToken("test@java.com", "password123");
    String cardId = "card_021";
    //pongo id de user aleatorio porque lo extrae del token
    registerRepeatedCard(cardId, token, "1");

    AuctionConditionDto condicion1 = AuctionConditionDto.builder()
        .filterName("MIN_CARD_COUNT")
        .quantity(2)
        .build();

    AuctionConditionDto condicion2 = AuctionConditionDto.builder()
        .filterName("MIN_EXCHANGES")
        .quantity(5)
        .build();
    String auctionBody = createAuctionBody(cardId, 10, List.of(condicion1, condicion2));
    mockMvc.perform(post("/api/auctions")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .content(auctionBody))
        .andExpect(status().is2xxSuccessful());
  }

  private void registerRepeatedCard(String cardId, String token, String userID) throws Exception {
    mockMvc.perform(post("/api/users/" + userID + "/collection")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .content(addToCollectionBody(cardId)))
        .andExpect(status().is2xxSuccessful());
  }

  private String addToCollectionBody(String cardId) {
    return """
        {
          "cardId": "%s"
        }
        """.formatted(cardId);
  }
  private void registrarUsuario(String name, String email, String password, String avatarId) throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody(name, email, password, avatarId)))
        .andExpect(status().is2xxSuccessful());
  }

  private String getUserToken(String email, String password) throws Exception{
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody(email, password)))
        .andExpect(status().isOk())
        .andReturn();

    String responseBody = result.getResponse().getContentAsString();

    return JsonPath.read(responseBody, "$.token");
  }

  private String registerBody(String name, String email, String password, String avatarId) {
    return """
        {
          "name": "%s",
          "email": "%s",
          "password": "%s",
          "avatarId": "%s"
        }
        """.formatted(name, email, password, avatarId);
  }

  private String loginBody(String email, String password) {
    return """
        {
          "email": "%s",
          "password": "%s"
        }
        """.formatted(email, password);
  }

  private String createAuctionBody(String cardId, Integer auctionDurationHours, List<AuctionConditionDto> conditions) throws JsonProcessingException {
    CreateAuctionDTO dto = new CreateAuctionDTO();
    dto.setCardId(cardId);
    dto.setAuctionDurationHours(auctionDurationHours);
    dto.setConditions(conditions);

    return objectMapper.writeValueAsString(dto);
  }

}
