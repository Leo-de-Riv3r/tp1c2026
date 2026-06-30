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

import java.time.LocalDateTime;
import java.util.List;

public class AuctionTests extends IntegrationTestBase {

  @Autowired
  private ExchangeRepository exchangeRepository;
  @Autowired
  private AuctionService auctionService;

  @Test
  void searchActiveAuctionsReturnsCreatedAuction() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    String cardId = "ARG4";

    addToCollection(seller.userId(), cardId, seller.token());

    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody(cardId, 24, List.of())))
        .andExpect(status().is2xxSuccessful());

    // El search activo excluye las subastas propias, así que busca otro user.
    Session buyer = register("buyer", "buyer@java.com", "password123");
    MvcResult res = mockMvc.perform(get("/api/auctions")
            .header("Authorization", "Bearer " + buyer.token()))
        .andExpect(status().isOk())
        .andReturn();

    String body = res.getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$.data")).size());
    assertEquals(4, (Integer) JsonPath.read(body, "$.data[0].cardNumber"));
  }

  @Test
  void getMyAuctionsReturnsCurrentUserAuctions() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");

    addToCollection(seller.userId(), "ARG4", seller.token());
    addToCollection(seller.userId(), "ARG5", seller.token());

    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("ARG4", 24, List.of())))
        .andExpect(status().is2xxSuccessful());

    mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("ARG5", 24, List.of())))
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
    addToCollection(seller.userId(), "ARG4", seller.token());

    MvcResult created = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("ARG4", 24, List.of())))
        .andReturn();

    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

    mockMvc.perform(delete("/api/auctions/" + auctionId)
            .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().is2xxSuccessful());

    MvcResult col = mockMvc.perform(get("/api/users/" + seller.userId() + "/collection")
        .header("Authorization", "Bearer " + seller.token())).andReturn();

    String colBody = col.getResponse().getContentAsString();
    Integer colQty = JsonPath.read(colBody, "$[0].quantity");
    Integer colAvail = JsonPath.read(colBody, "$[0].available");
    assertEquals(0, colQty - colAvail);
  }

  @Test
  void markingInterestedTwiceDoesNotDuplicateUser() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    MvcResult created = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("ARG4", 24, List.of())))
        .andReturn();

    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

    Session buyer = register("buyer", "buyer@java.com", "password123");
    for (int i = 0; i < 2; i++) {
      mockMvc.perform(post("/api/auctions/" + auctionId + "/interested")
              .header("Authorization", "Bearer " + buyer.token()))
          .andExpect(status().is2xxSuccessful());
    }

    List<User> interested = auctionService.getAuctionById(auctionId).getInterestedUsers();
    assertEquals(1, interested.size());
    assertEquals(buyer.userId(), interested.get(0).getId());
  }

  @Test
  void myOffersReturnsBidsPlacedByCurrentUser() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    MvcResult created = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("ARG4", 24, List.of())))
        .andReturn();
    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollectionN(bidder.userId(), "ARG5", 2, bidder.token());

    String offerBody = """
        { "items": [ { "cardId": "ARG5", "amount": 1 } ] }
        """;

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
    String cardId = "ARG4";
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
    addToCollection(seller.userId(), "ARG4", seller.token());

    AuctionConditionDto repCond = AuctionConditionDto.builder().filterName("MIN_REPUTATION").quantity(3).build();
    AuctionConditionDto exchCond = AuctionConditionDto.builder().filterName("MIN_EXCHANGES").quantity(2).build();

    MvcResult created = mockMvc.perform(post("/api/auctions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + seller.token())
            .content(createAuctionBody("ARG4", 24, List.of(repCond, exchCond))))
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
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "ARG1", bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"ARG1\", \"amount\": 1 } ] }"))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void minReputationBlocksBidderWithNullRating() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "ARG4",
        List.of(AuctionConditionDto.builder().filterName("MIN_REPUTATION").quantity(3).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "ARG1", bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"ARG1\", \"amount\": 1 } ] }"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void minReputationAllowsBidderWithSufficientRating() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "ARG4",
        List.of(AuctionConditionDto.builder().filterName("MIN_REPUTATION").quantity(3).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "ARG1", bidder.token());
    setUserRating(bidder.userId(), 4.0);

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"ARG1\", \"amount\": 1 } ] }"))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void minExchangesBlocksBidderWithZeroExchanges() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "ARG4",
        List.of(AuctionConditionDto.builder().filterName("MIN_EXCHANGES").quantity(2).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "ARG1", bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"ARG1\", \"amount\": 1 } ] }"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void minExchangesAllowsBidderWithSufficientExchanges() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "ARG4",
        List.of(AuctionConditionDto.builder().filterName("MIN_EXCHANGES").quantity(2).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "ARG1", bidder.token());
    setUserExchangesAmount(bidder.userId(), 3);

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"ARG1\", \"amount\": 1 } ] }"))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void minCardCountBlocksBidderOfferingTooFewCards() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "ARG4",
        List.of(AuctionConditionDto.builder().filterName("MIN_CARD_COUNT").quantity(3).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollectionN(bidder.userId(), "ARG1", 3, bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"ARG1\", \"amount\": 1 } ] }"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void minCardCountAllowsBidderOfferingEnoughCards() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "ARG4",
        List.of(AuctionConditionDto.builder().filterName("MIN_CARD_COUNT").quantity(3).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollectionN(bidder.userId(), "ARG1", 3, bidder.token());

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"ARG1\", \"amount\": 3 } ] }"))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void minCategoryBlocksBidderOfferingCommonWhenEpicRequired() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "ARG4",
        List.of(AuctionConditionDto.builder().filterName("MIN_CATEGORY").value(com.tacs.tp1c2026.entities.enums.Category.fromValue("EPICO")).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "ARG1", bidder.token()); // COMUN

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"ARG1\", \"amount\": 1 } ] }"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void minCategoryAllowsBidderOfferingEpicOrHigher() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());

    String auctionId = createAuctionWithConditionsAndGetId(seller.token(), "ARG4",
        List.of(AuctionConditionDto.builder().filterName("MIN_CATEGORY").value(com.tacs.tp1c2026.entities.enums.Category.fromValue("EPICO")).build()));

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "FWC1", bidder.token()); // EPICO

    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"FWC1\", \"amount\": 1 } ] }"))
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
        { "items": [ { "cardId": "%s", "amount": %d } ] }
        """.formatted(cardId, amount);
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
    return JsonPath.read(res.getResponse().getContentAsString(), "$.offers[0].id");
  }

  private Integer compromisedCount(String userId, String cardId, String token) throws Exception {
    MvcResult res = mockMvc.perform(get("/api/users/" + userId + "/collection")
        .header("Authorization", "Bearer " + token))
        .andReturn();
    String body = res.getResponse().getContentAsString();
    List<?> all = JsonPath.read(body, "$");
    for (int i = 0; i < all.size(); i++) {
      String cid = JsonPath.read(body, "$[" + i + "].cardId");
      if (cardId.equals(cid)) {
        Integer quantity = JsonPath.read(body, "$[" + i + "].quantity");
        Integer available = JsonPath.read(body, "$[" + i + "].available");
        return quantity - available;
      }
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
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "BRA5", bidder.token());
    placeBid(bidder.token(), auctionId, "BRA5", 1);
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
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "BRA5", bidder.token());
    placeBid(bidder.token(), auctionId, "BRA5", 1);
    String offerId = firstOfferId(auctionId, seller.token());

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/accept")
        .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().isOk());

    // Bidder pierde la ofrecida y recibe la subastada
    assertEquals(0, quantityInCollection(bidder.userId(), "BRA5", bidder.token()));
    assertEquals(1, quantityInCollection(bidder.userId(), "ARG4", bidder.token()));

    // Seller pierde la subastada y recibe la ofrecida
    assertEquals(0, quantityInCollection(seller.userId(), "ARG4", seller.token()));
    assertEquals(1, quantityInCollection(seller.userId(), "BRA5", seller.token()));
  }

  @Test
  void acceptAuctionOfferForbiddenForNonPublisher() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "BRA5", bidder.token());
    placeBid(bidder.token(), auctionId, "BRA5", 1);
    String offerId = firstOfferId(auctionId, seller.token());

    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/accept")
        .header("Authorization", "Bearer " + bidder.token()))
        .andExpect(status().isForbidden());
  }

  @Test
  void closeExpiredAuctionWithBestOfferAwardsAndCreatesExchange() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "BRA5", bidder.token());
    placeBid(bidder.token(), auctionId, "BRA5", 1);
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
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    assertEquals(1, compromisedCount(seller.userId(), "ARG4", seller.token()));

    auctionService.closeExpiredAuction(auctionId);

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + seller.token()))
        .andReturn();
    assertEquals("CANCELLED", JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));
    assertEquals(0, compromisedCount(seller.userId(), "ARG4", seller.token()));
    assertTrue(exchangeRepository.findAll().isEmpty());
  }

  /**
   * Camino del cron en producción: subasta REALMENTE vencida, con oferta pero SIN bestOffer manual.
   * Antes del fix, bestOffer == null → cancel() (que tira por estar expirada) → la subasta quedaba
   * colgada en ACTIVE. Ahora se auto-selecciona la mejor oferta y se adjudica.
   */
  @Test
  void closeTrulyExpiredAuctionWithoutManualBestAutoSelectsAndAwards() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "BRA5", bidder.token());
    placeBid(bidder.token(), auctionId, "BRA5", 1);

    expireAuction(auctionId);
    auctionService.closeExpiredAuction(auctionId);

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + seller.token()))
        .andReturn();
    assertEquals("AWARDED", JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));
    assertEquals(1, exchangeRepository.findAll().size());
    assertEquals(1, userRepository.findById(seller.userId()).get().getExchangesAmount());
    assertEquals(1, userRepository.findById(bidder.userId()).get().getExchangesAmount());
  }

  /**
   * Subasta REALMENTE vencida y sin ofertas: se cierra sin ganador (CANCELLED) liberando el
   * commit del publisher, sin que el guard de expiración de cancel() la haga fallar.
   */
  @Test
  void closeTrulyExpiredAuctionWithoutOffersCancelsAndReleasesCommit() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");
    assertEquals(1, compromisedCount(seller.userId(), "ARG4", seller.token()));

    expireAuction(auctionId);
    auctionService.closeExpiredAuction(auctionId);

    MvcResult detail = mockMvc.perform(get("/api/auctions/" + auctionId)
        .header("Authorization", "Bearer " + seller.token()))
        .andReturn();
    assertEquals("CANCELLED", JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));
    assertEquals(0, compromisedCount(seller.userId(), "ARG4", seller.token()));
  }

  /** Fuerza el vencimiento de una subasta backdateando su closeDate vía Mongo. */
  private void expireAuction(String auctionId) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(auctionId)),
        Update.update("closeDate", LocalDateTime.now().minusHours(1)),
        "auctions");
  }

  @Test
  void rejectOfferReleasesBidderCommit() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "BRA5", bidder.token());
    placeBid(bidder.token(), auctionId, "BRA5", 1);
    assertEquals(1, compromisedCount(bidder.userId(), "BRA5", bidder.token()));

    String offerId = firstOfferId(auctionId, seller.token());
    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/reject")
        .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().isOk());

    assertEquals(0, compromisedCount(bidder.userId(), "BRA5", bidder.token()));
  }

  /**
   * Regresión: cancelar una oferta de otro usuario debe devolver 403 con body ApiError.
   * Antes daba 500 porque AuctionOffer.validateCreator tiraba IllegalArgumentException, que
   * no extendía CustomException y caía en handleUnexpectedError.
   */
  @Test
  void cancelOfferByNonCreatorReturnsForbidden() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "BRA5", bidder.token());
    placeBid(bidder.token(), auctionId, "BRA5", 1);
    String offerId = firstOfferId(auctionId, seller.token());

    Session stranger = register("stranger", "stranger@java.com", "password123");

    String body = mockMvc.perform(delete("/api/auctions/" + auctionId + "/offers/" + offerId)
            .header("Authorization", "Bearer " + stranger.token()))
        .andExpect(status().isForbidden())
        .andReturn().getResponse().getContentAsString();

    assertEquals(403, (int) JsonPath.read(body, "$.status"));
    assertEquals("Forbidden", JsonPath.read(body, "$.error"));
    assertEquals("Only the offer creator can perform this operation", JsonPath.read(body, "$.message"));
  }

  @Test
  void setBestOfferDoesNotCloseAuction() throws Exception {
    Session seller = register("seller", "seller@java.com", "password123");
    addToCollection(seller.userId(), "ARG4", seller.token());
    String auctionId = createAuctionAndGetId(seller.token(), "ARG4");

    Session bidder = register("bidder", "bidder@java.com", "password123");
    addToCollection(bidder.userId(), "BRA5", bidder.token());
    placeBid(bidder.token(), auctionId, "BRA5", 1);
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
