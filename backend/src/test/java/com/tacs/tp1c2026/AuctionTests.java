package com.tacs.tp1c2026;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.tacs.tp1c2026.entities.user.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.dto.auction.input.AuctionConditionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDto;
import com.tacs.tp1c2026.entities.exchange.Exchange;
import com.tacs.tp1c2026.repositories.ExchangeRepository;
import com.tacs.tp1c2026.services.AuctionService;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

public class AuctionTests extends IntegrationTestBase {

  @Autowired
  private ExchangeRepository exchangeRepository;
  @Autowired
  private AuctionService auctionService;

  @Test
  void searchActiveAuctionsReturnsCreatedAuction() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    String cardId = "card_021";

    addToCollection(seller.userId(), cardId, seller.token());

    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody(cardId, 24, List.of())))
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

    MvcResult res = mockMvc.perform(get("/api/auctions")
            .param("userId", seller.userId())
            .header("Authorization", "Bearer " + seller.token()))
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

    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

    mockMvc.perform(delete("/api/auctions/" + auctionId)
            .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().is2xxSuccessful());

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
    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollectionN(bidder.userId(), "card_022", 2, bidder.token());

    String offerBody = """
        { "auctionId": "%s", "items": [ { "cardId": "card_022", "amount": 1 } ] }
        """.formatted(auctionId);

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content(offerBody))
        .andExpect(status().is2xxSuccessful());

    MvcResult res = mockMvc.perform(get("/api/auctions/offers")
            .header("Authorization", "Bearer " + bidder.token()))
        .andExpect(status().isOk())
        .andReturn();

    String body = res.getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$")).size());
    assertEquals(auctionId, JsonPath.read(body, "$[0].auctionId"));

    MvcResult sellerRes = mockMvc.perform(get("/api/auctions/offers")
            .header("Authorization", "Bearer " + seller.token()))
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

  // ===== Tests de condiciones =====

  @Test
  void conditionsAreStoredAndReturnedInAuctionResponse() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    AuctionConditionDto repCond = AuctionConditionDto.builder().filterName("MIN_REPUTATION").quantity(3).build();
    AuctionConditionDto exchCond = AuctionConditionDto.builder().filterName("MIN_EXCHANGES").quantity(2).build();

    MvcResult created = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("card_021", 24, List.of(repCond, exchCond))))
        .andExpect(status().is2xxSuccessful())
        .andReturn();

    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
            .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().isOk())
        .andReturn();

    String body = detail.getResponse().getContentAsString();
    List<?> conditions = JsonPath.read(body, "$.conditions");
    assertEquals(2, conditions.size());
    assertEquals("MIN_REPUTATION", JsonPath.read(body, "$.conditions[0].filterName"));
    assertEquals(3, (Integer) JsonPath.read(body, "$.conditions[0].quantity"));
    assertEquals("MIN_EXCHANGES", JsonPath.read(body, "$.conditions[1].filterName"));
    assertEquals(2, (Integer) JsonPath.read(body, "$.conditions[1].quantity"));
  }

  @Test
  void auctionWithoutConditionsAllowsAnyBidder() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "card_021");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_003", bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"auctionId\": \"%s\", \"items\": [ { \"cardId\": \"card_003\", \"amount\": 1 } ] }".formatted(auctionId)))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void minReputationBlocksBidderWithNullRating() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "card_021",
        List.of(AuctionConditionDto.builder().filterName("MIN_REPUTATION").quantity(3).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_003", bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"auctionId\": \"%s\", \"items\": [ { \"cardId\": \"card_003\", \"amount\": 1 } ] }".formatted(auctionId)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void minReputationAllowsBidderWithSufficientRating() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "card_021",
        List.of(AuctionConditionDto.builder().filterName("MIN_REPUTATION").quantity(3).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_003", bidder.token());
    setUserRating(bidder.userId(), 4.0);

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"auctionId\": \"%s\", \"items\": [ { \"cardId\": \"card_003\", \"amount\": 1 } ] }".formatted(auctionId)))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void minExchangesBlocksBidderWithZeroExchanges() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "card_021",
        List.of(AuctionConditionDto.builder().filterName("MIN_EXCHANGES").quantity(2).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_003", bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"auctionId\": \"%s\", \"items\": [ { \"cardId\": \"card_003\", \"amount\": 1 } ] }".formatted(auctionId)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void minExchangesAllowsBidderWithSufficientExchanges() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "card_021",
        List.of(AuctionConditionDto.builder().filterName("MIN_EXCHANGES").quantity(2).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_003", bidder.token());
    setUserExchangesAmount(bidder.userId(), 3);

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"auctionId\": \"%s\", \"items\": [ { \"cardId\": \"card_003\", \"amount\": 1 } ] }".formatted(auctionId)))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void minCardCountBlocksBidderOfferingTooFewCards() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "card_021",
        List.of(AuctionConditionDto.builder().filterName("MIN_CARD_COUNT").quantity(3).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollectionN(bidder.userId(), "card_003", 3, bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"auctionId\": \"%s\", \"items\": [ { \"cardId\": \"card_003\", \"amount\": 1 } ] }".formatted(auctionId)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void minCardCountAllowsBidderOfferingEnoughCards() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "card_021",
        List.of(AuctionConditionDto.builder().filterName("MIN_CARD_COUNT").quantity(3).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollectionN(bidder.userId(), "card_003", 3, bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"auctionId\": \"%s\", \"items\": [ { \"cardId\": \"card_003\", \"amount\": 3 } ] }".formatted(auctionId)))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void minCategoryBlocksBidderOfferingCommonWhenEpicRequired() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "card_021",
        List.of(AuctionConditionDto.builder().filterName("MIN_CATEGORY").value(com.tacs.tp1c2026.entities.enums.Category.fromValue("EPICO")).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_003", bidder.token()); // COMUN

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"auctionId\": \"%s\", \"items\": [ { \"cardId\": \"card_003\", \"amount\": 1 } ] }".formatted(auctionId)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void minCategoryAllowsBidderOfferingEpicOrHigher() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "card_021",
        List.of(AuctionConditionDto.builder().filterName("MIN_CATEGORY").value(com.tacs.tp1c2026.entities.enums.Category.fromValue("EPICO")).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_001", bidder.token()); // EPICO

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"auctionId\": \"%s\", \"items\": [ { \"cardId\": \"card_001\", \"amount\": 1 } ] }".formatted(auctionId)))
        .andExpect(status().is2xxSuccessful());
  }

  // ===== Helpers exclusivos de AuctionTests (no incluidos en IntegrationTestBase) =====

  private String createAuctionWithConditionsAndGetId(String token, String cardId, List<AuctionConditionDto> conditions) throws Exception {
    MvcResult res = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(createAuctionBody(cardId, 24, conditions)))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
  }

  private void setUserRating(String userId, double rating) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(userId)),
        new Update().set("rating", rating),
        User.class
    );
  }

  private void setUserExchangesAmount(String userId, int amount) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(userId)),
        new Update().set("exchangesAmount", amount),
        User.class
    );
  }

  private String createAuctionBody(String cardId, Integer auctionDurationHours, List<AuctionConditionDto> conditions) throws JsonProcessingException {
    CreateAuctionDto dto = new CreateAuctionDto();
    dto.setCardId(cardId);
    dto.setAuctionDurationHours(auctionDurationHours);
    dto.setConditions(conditions);
    return objectMapper.writeValueAsString(dto);
  }

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
    return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
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

  /** Devuelve la cantidad de {@code cardId} en la colección del usuario, o 0 si no la tiene. */
  private int quantityInCollection(String userId, String cardId, String token) throws Exception {
    MvcResult res = mockMvc.perform(get("/api/users/" + userId + "/collection")
        .header("Authorization", "Bearer " + token))
        .andReturn();
    String body = res.getResponse().getContentAsString();
    List<?> all = JsonPath.read(body, "$");
    for (int i = 0; i < all.size(); i++) {
      String cid = JsonPath.read(body, "$[" + i + "].cardId");
      if (cardId.equals(cid)) {
        Integer qty = JsonPath.read(body, "$[" + i + "].quantity");
        return qty == null ? 0 : qty;
      }
    }
    return 0;
  }

  private void registrarUsuario(String name, String email, String password, String avatarId) throws Exception {
    String body = "{ \"name\": \"" + name + "\", \"email\": \"" + email
        + "\", \"password\": \"" + password + "\", \"avatarId\": \"" + avatarId + "\" }";
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().is2xxSuccessful());
  }

  private String getUserToken(String email, String password) throws Exception {
    return login(email, password).token();
  }

  private void registerRepeatedCard(String cardId, String token, String userId) throws Exception {
    addToCollection(userId, cardId, token);
  }

  private String loginBody(String email, String password) {
    return "{ \"email\": \"" + email + "\", \"password\": \"" + password + "\" }";
  }

  // ===== Tests del refactor: accept manual / cron close / reject / best =====

  @Test
  void acceptAuctionOfferFinalizesAndCreatesExchange() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "card_021");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_050", bidder.token());
    placeBid(bidder.token(), auctionId, "card_050", 1);
    String offerId = firstOfferId(auctionId, seller.token());

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/accept")
        .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().isOk());

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + seller.token()))
        .andReturn();
    assertEquals("AWARDED", JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));

    List<Exchange> exchanges = exchangeRepository.findAll();
    assertEquals(1, exchanges.size());
    assertEquals("SUBASTA", exchanges.get(0).getOrigin().getType().name());

    assertEquals(1, userRepository.findById(seller.userId()).get().getExchangesAmount());
    assertEquals(1, userRepository.findById(bidder.userId()).get().getExchangesAmount());
  }

  /**
   * Regresión BE: al aceptar una oferta de subasta, las cantidades en las colecciones
   * de ambos lados se deben actualizar. El bug original (causa: @DocumentReference no hidrata
   * dentro de subdocs embebidos) dejaba la card ofrecida intacta en la colección del bidder.
   */
  @Test
  void acceptAuctionOfferTransfersCardsBetweenCollections() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "card_021");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_050", bidder.token());
    placeBid(bidder.token(), auctionId, "card_050", 1);
    String offerId = firstOfferId(auctionId, seller.token());

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/accept")
        .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().isOk());

    // Bidder pierde la ofrecida y recibe la subastada
    assertEquals(0, quantityInCollection(bidder.userId(), "card_050", bidder.token()));
    assertEquals(1, quantityInCollection(bidder.userId(), "card_021", bidder.token()));

    // Seller pierde la subastada y recibe la ofrecida
    assertEquals(0, quantityInCollection(seller.userId(), "card_021", seller.token()));
    assertEquals(1, quantityInCollection(seller.userId(), "card_050", seller.token()));
  }

  @Test
  void acceptAuctionOfferForbiddenForNonPublisher() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "card_021");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_050", bidder.token());
    placeBid(bidder.token(), auctionId, "card_050", 1);
    String offerId = firstOfferId(auctionId, seller.token());

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/accept")
        .header("Authorization", "Bearer " + bidder.token()))
        .andExpect(status().isForbidden());
  }

  @Test
  void closeExpiredAuctionWithBestOfferAwardsAndCreatesExchange() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "card_021");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_050", bidder.token());
    placeBid(bidder.token(), auctionId, "card_050", 1);
    String offerId = firstOfferId(auctionId, seller.token());

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/best")
        .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().isOk());

    auctionService.closeExpiredAuction(auctionId);

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + seller.token()))
        .andReturn();
    assertEquals("AWARDED", JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));

    List<Exchange> exchanges = exchangeRepository.findAll();
    assertEquals(1, exchanges.size());
    assertEquals("SUBASTA", exchanges.get(0).getOrigin().getType().name());

    assertEquals(1, userRepository.findById(seller.userId()).get().getExchangesAmount());
    assertEquals(1, userRepository.findById(bidder.userId()).get().getExchangesAmount());
  }

  @Test
  void closeExpiredAuctionWithoutOffersCancelsAndReleasesCommit() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "card_021");

    assertEquals(1, compromisedCount(seller.userId(), "card_021", seller.token()));

    auctionService.closeExpiredAuction(auctionId);

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + seller.token()))
        .andReturn();
    assertEquals("CANCELLED", JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));
    assertEquals(0, compromisedCount(seller.userId(), "card_021", seller.token()));
    assertTrue(exchangeRepository.findAll().isEmpty());
  }

  @Test
  void rejectOfferReleasesBidderCommit() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "card_021");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_050", bidder.token());
    placeBid(bidder.token(), auctionId, "card_050", 1);
    assertEquals(1, compromisedCount(bidder.userId(), "card_050", bidder.token()));

    String offerId = firstOfferId(auctionId, seller.token());
    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/reject")
        .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().isOk());

    assertEquals(0, compromisedCount(bidder.userId(), "card_050", bidder.token()));
  }

  @Test
  void setBestOfferDoesNotCloseAuction() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "card_021", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "card_021");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "card_050", bidder.token());
    placeBid(bidder.token(), auctionId, "card_050", 1);
    String offerId = firstOfferId(auctionId, seller.token());

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/best")
        .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().isOk());

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + seller.token()))
        .andReturn();
    String body = detail.getResponse().getContentAsString();
    assertEquals("ACTIVE", JsonPath.read(body, "$.status"));
    assertNotNull(JsonPath.read(body, "$.bestOffer"));
    assertTrue(exchangeRepository.findAll().isEmpty());
  }
}
