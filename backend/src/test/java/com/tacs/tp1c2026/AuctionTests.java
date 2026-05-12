package com.tacs.tp1c2026;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.dto.auction.input.AuctionConditionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;
import com.tacs.tp1c2026.entities.exchange.Exchange;

import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.CardRepository;
import com.tacs.tp1c2026.repositories.ExchangeRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.services.AuctionService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
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
  @Autowired
  private PublicationRepository publicationRepository;
  @Autowired
  private ExchangeRepository exchangeRepository;
  @Autowired
  private MongoTemplate mongoTemplate;
  @Autowired
  private AuctionService auctionService;

  @BeforeEach
  void setup() throws IOException {
    userRepository.deleteAll();
    cardRepository.deleteAll();
    auctionRepository.deleteAll();
    publicationRepository.deleteAll();
    mongoTemplate.dropCollection("proposals");
    mongoTemplate.dropCollection("exchanges");
    try (InputStream inputStream = getClass().getResourceAsStream("/catalog.json")) {
      List<Card> cards = objectMapper.readValue(inputStream, new TypeReference<List<Card>>() {});
      cardRepository.saveAll(cards);
      System.out.println("✅ Base de datos de prueba inicializada con " + cards.size() + " cartas.");
    }
  }

  @Test
  void searchActiveAuctionsReturnsCreatedAuction() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    String cardId = "card_021";

    // Usamos el helper de la clase base y el ID real del usuario
    addToCollection(seller.userId(), cardId, seller.token());

    String auctionBody = createAuctionBody(cardId, 24, List.of());
    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(auctionBody))
        .andExpect(status().is2xxSuccessful());

    MvcResult res = mockMvc.perform(get("/api/auctions")
            .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().isOk())
        .andReturn();

    String body = res.getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$.data")).size());
    assertEquals(21, (Integer) JsonPath.read(body, "$.data[0].cardNumber"));
  }

  @Test
  void getMyAuctionsReturnsCurrentUserAuctions() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");

    addToCollection(seller.userId(), "card_021", seller.token());
    addToCollection(seller.userId(), "card_022", seller.token());

    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("card_021", 24, List.of())))
        .andExpect(status().is2xxSuccessful());

    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("card_022", 24, List.of())))
        .andExpect(status().is2xxSuccessful());

    String userId = JsonPath.read(mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody("seller@java.com", "password123")))
        .andReturn().getResponse().getContentAsString(), "$.user.id");
    MvcResult res = mockMvc.perform(get("/api/auctions")
            .param("userId", userId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn();

    assertEquals(2, ((java.util.List<?>) JsonPath.read(res.getResponse().getContentAsString(), "$.data")).size());
  }

  @Test
  void cancelAuctionReleasesCommittedCard() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    MvcResult created = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("card_021", 24, List.of())))
        .andReturn();

    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.auctionId");

    mockMvc.perform(delete("/api/auctions/" + auctionId)
            .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().is2xxSuccessful());

    // Al usar el objeto seller.userId(), ya no necesitamos volver a loguearnos para leer el ID
    MvcResult col = mockMvc.perform(get("/api/users/" + seller.userId() + "/collection")
        .header("Authorization", "Bearer " + seller.token())).andReturn();

    assertEquals(0, (Integer) JsonPath.read(col.getResponse().getContentAsString(), "$[0].compromisedCount"));
  }

  @Test
  void myOffersReturnsBidsPlacedByCurrentUser() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    MvcResult created = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("card_021", 24, List.of())))
        .andReturn();
    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.auctionId");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollectionN(bidder.userId(), "card_022", 2, bidder.token()); // Usa el addToCollectionN de la clase base

    String offerBody = """
        { "auctionId": "%s", "items": [ { "cardId": "card_022", "amount": 1 } ] }
        """.formatted(auctionId);

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content(offerBody))
        .andExpect(status().is2xxSuccessful());

    MvcResult res = mockMvc.perform(get("/api/auctions/offers")
            .header("Authorization", "Bearer " + bidderToken))
        .andExpect(status().isOk())
        .andReturn();

    String body = res.getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$")).size());
    assertEquals(auctionId, JsonPath.read(body, "$[0].auctionId"));

    MvcResult sellerRes = mockMvc.perform(get("/api/auctions/offers")
            .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isOk())
        .andReturn();

    assertEquals(0, ((java.util.List<?>) JsonPath.read(sellerRes.getResponse().getContentAsString(), "$")).size());
  }

  @Test
  void publishCardForAuction() throws Exception {
    Session user = register("testUser", "test@java.com", "password123");
    String cardId = "card_021";
    addToCollection(user.userId(), cardId, user.token());

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
            .header("Authorization", "Bearer " + user.token())
            .content(auctionBody))
        .andExpect(status().is2xxSuccessful());
  }

  // Conservamos solo el helper exclusivo de Auction, el resto se borró porque se hereda
  private String createAuctionBody(String cardId, Integer auctionDurationHours, List<AuctionConditionDto> conditions) throws JsonProcessingException {
    CreateAuctionDTO dto = new CreateAuctionDTO();
    dto.setCardId(cardId);
    dto.setAuctionDurationHours(auctionDurationHours);
    dto.setConditions(conditions);

    // Se usa el objectMapper protegido de la clase base
    return objectMapper.writeValueAsString(dto);
  }

  // ===== Helpers para los tests de accept/close/reject/best =====

  private String userIdFromLogin(String email, String password) throws Exception {
    return JsonPath.read(mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginBody(email, password)))
        .andReturn().getResponse().getContentAsString(), "$.user.id");
  }

  private String createAuctionAndGetId(String token, String cardId) throws Exception {
    MvcResult res = mockMvc.perform(post("/api/auctions")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .content(createAuctionBody(cardId, 24, List.of())))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    return JsonPath.read(res.getResponse().getContentAsString(), "$.auctionId");
  }

  private void placeBid(String bidderToken, String auctionId, String cardId, int amount) throws Exception {
    String offerBody = """
        { "auctionId": "%s", "items": [ { "cardId": "%s", "amount": %d } ] }
        """.formatted(auctionId, cardId, amount);
    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + bidderToken)
        .content(offerBody))
        .andExpect(status().is2xxSuccessful());
  }

  private String firstOfferId(String auctionId, String token) throws Exception {
    MvcResult res = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn();
    return JsonPath.read(res.getResponse().getContentAsString(), "$.offers[0].offerId");
  }

  private Integer compromisedCount(String userId, String cardId, String token) throws Exception {
    MvcResult res = mockMvc.perform(get("/api/users/" + userId + "/collection")
        .header("Authorization", "Bearer " + token))
        .andReturn();
    String body = res.getResponse().getContentAsString();
    List<?> all = JsonPath.read(body, "$");
    for (int i = 0; i < all.size(); i++) {
      String cid = JsonPath.read(body, "$[" + i + "].cardId");
      if (cardId.equals(cid)) return JsonPath.read(body, "$[" + i + "].compromisedCount");
    }
    return null;
  }

  // ===== Tests del refactor: accept manual / cron close / reject / best =====

  @Test
  void acceptAuctionOfferFinalizesAndCreatesExchange() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String sellerToken = getUserToken("seller@java.com", "password123");
    String sellerId = userIdFromLogin("seller@java.com", "password123");
    registerRepeatedCard("card_021", sellerToken, sellerId);
    String auctionId = createAuctionAndGetId(sellerToken, "card_021");

    registrarUsuario("bidder", "bidder@java.com", "password123", "avatar1");
    String bidderToken = getUserToken("bidder@java.com", "password123");
    String bidderId = userIdFromLogin("bidder@java.com", "password123");
    registerRepeatedCard("card_050", bidderToken, bidderId);
    placeBid(bidderToken, auctionId, "card_050", 1);
    String offerId = firstOfferId(auctionId, sellerToken);

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/accept")
        .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isNoContent());

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + sellerToken))
        .andReturn();
    assertEquals("AWARDED", JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));

    List<Exchange> exchanges = exchangeRepository.findAll();
    assertEquals(1, exchanges.size());
    assertEquals("SUBASTA", exchanges.get(0).getOrigin().getType().name());

    assertEquals(1, userRepository.findById(sellerId).get().getExchangesAmount());
    assertEquals(1, userRepository.findById(bidderId).get().getExchangesAmount());
  }

  @Test
  void acceptAuctionOfferForbiddenForNonPublisher() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String sellerToken = getUserToken("seller@java.com", "password123");
    String sellerId = userIdFromLogin("seller@java.com", "password123");
    registerRepeatedCard("card_021", sellerToken, sellerId);
    String auctionId = createAuctionAndGetId(sellerToken, "card_021");

    registrarUsuario("bidder", "bidder@java.com", "password123", "avatar1");
    String bidderToken = getUserToken("bidder@java.com", "password123");
    String bidderId = userIdFromLogin("bidder@java.com", "password123");
    registerRepeatedCard("card_050", bidderToken, bidderId);
    placeBid(bidderToken, auctionId, "card_050", 1);
    String offerId = firstOfferId(auctionId, sellerToken);

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/accept")
        .header("Authorization", "Bearer " + bidderToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void closeExpiredAuctionWithBestOfferAwardsAndCreatesExchange() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String sellerToken = getUserToken("seller@java.com", "password123");
    String sellerId = userIdFromLogin("seller@java.com", "password123");
    registerRepeatedCard("card_021", sellerToken, sellerId);
    String auctionId = createAuctionAndGetId(sellerToken, "card_021");

    registrarUsuario("bidder", "bidder@java.com", "password123", "avatar1");
    String bidderToken = getUserToken("bidder@java.com", "password123");
    String bidderId = userIdFromLogin("bidder@java.com", "password123");
    registerRepeatedCard("card_050", bidderToken, bidderId);
    placeBid(bidderToken, auctionId, "card_050", 1);
    String offerId = firstOfferId(auctionId, sellerToken);

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/best")
        .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isNoContent());

    auctionService.closeExpiredAuction(auctionId);

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + sellerToken))
        .andReturn();
    assertEquals("AWARDED", JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));

    List<Exchange> exchanges = exchangeRepository.findAll();
    assertEquals(1, exchanges.size());
    assertEquals("SUBASTA", exchanges.get(0).getOrigin().getType().name());

    assertEquals(1, userRepository.findById(sellerId).get().getExchangesAmount());
    assertEquals(1, userRepository.findById(bidderId).get().getExchangesAmount());
  }

  @Test
  void closeExpiredAuctionWithoutOffersCancelsAndReleasesCommit() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String sellerToken = getUserToken("seller@java.com", "password123");
    String sellerId = userIdFromLogin("seller@java.com", "password123");
    registerRepeatedCard("card_021", sellerToken, sellerId);
    String auctionId = createAuctionAndGetId(sellerToken, "card_021");

    assertEquals(1, compromisedCount(sellerId, "card_021", sellerToken));

    auctionService.closeExpiredAuction(auctionId);

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + sellerToken))
        .andReturn();
    assertEquals("CANCELLED", JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));
    assertEquals(0, compromisedCount(sellerId, "card_021", sellerToken));
    assertTrue(exchangeRepository.findAll().isEmpty());
  }

  @Test
  void rejectOfferReleasesBidderCommit() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String sellerToken = getUserToken("seller@java.com", "password123");
    String sellerId = userIdFromLogin("seller@java.com", "password123");
    registerRepeatedCard("card_021", sellerToken, sellerId);
    String auctionId = createAuctionAndGetId(sellerToken, "card_021");

    registrarUsuario("bidder", "bidder@java.com", "password123", "avatar1");
    String bidderToken = getUserToken("bidder@java.com", "password123");
    String bidderId = userIdFromLogin("bidder@java.com", "password123");
    registerRepeatedCard("card_050", bidderToken, bidderId);
    placeBid(bidderToken, auctionId, "card_050", 1);
    assertEquals(1, compromisedCount(bidderId, "card_050", bidderToken));

    String offerId = firstOfferId(auctionId, sellerToken);
    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/reject")
        .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isNoContent());

    assertEquals(0, compromisedCount(bidderId, "card_050", bidderToken));
  }

  @Test
  void setBestOfferDoesNotCloseAuction() throws Exception {
    registrarUsuario("seller", "seller@java.com", "password123", "avatar1");
    String sellerToken = getUserToken("seller@java.com", "password123");
    String sellerId = userIdFromLogin("seller@java.com", "password123");
    registerRepeatedCard("card_021", sellerToken, sellerId);
    String auctionId = createAuctionAndGetId(sellerToken, "card_021");

    registrarUsuario("bidder", "bidder@java.com", "password123", "avatar1");
    String bidderToken = getUserToken("bidder@java.com", "password123");
    String bidderId = userIdFromLogin("bidder@java.com", "password123");
    registerRepeatedCard("card_050", bidderToken, bidderId);
    placeBid(bidderToken, auctionId, "card_050", 1);
    String offerId = firstOfferId(auctionId, sellerToken);

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/best")
        .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isNoContent());

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + sellerToken))
        .andReturn();
    String body = detail.getResponse().getContentAsString();
    assertEquals("ACTIVE", JsonPath.read(body, "$.status"));
    assertNotNull(JsonPath.read(body, "$.bestOffer"));
    assertTrue(exchangeRepository.findAll().isEmpty());
  }

}
