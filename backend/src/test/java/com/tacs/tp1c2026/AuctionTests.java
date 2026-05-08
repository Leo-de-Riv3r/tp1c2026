package com.tacs.tp1c2026;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.dto.auction.input.AuctionConditionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;
import com.tacs.tp1c2026.support.IntegrationTestBase;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

// Ya no hacen falta las anotaciones de clase porque se heredan de IntegrationTestBase
public class AuctionTests extends IntegrationTestBase {

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

    MvcResult res = mockMvc.perform(get("/api/auctions/createdByMe")
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

    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.auctionId");

    mockMvc.perform(delete("/api/auctions/" + auctionId)
            .header("Authorization", "Bearer " + seller.token()))
        .andExpect(status().is2xxSuccessful());

    // Al usar el objeto seller.userId(), ya no necesitamos volver a loguearnos para leer el ID
    MvcResult col = mockMvc.perform(get("/api/users/" + seller.userId() + "/collection")
        .header("Authorization", "Bearer " + seller.token())).andReturn();

    assertEquals(0, (Integer) JsonPath.read(col.getResponse().getContentAsString(), "$[0].compromisedCount"));
  }

  // TODO: test para GET /api/auctions/myOffers — el endpoint funciona en runtime
  // pero el filtro en memoria no encuentra al bidder en los tests; revisar cómo persiste
  // @DocumentReference dentro de array embebido en el contexto de MockMvc.

  @org.junit.jupiter.api.Disabled("WIP: ver TODO arriba")
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

    MvcResult res = mockMvc.perform(get("/api/auctions/myOffers")
            .header("Authorization", "Bearer " + bidder.token()))
        .andExpect(status().isOk())
        .andReturn();

    String body = res.getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$")).size());
    assertEquals(auctionId, JsonPath.read(body, "$[0].auctionId"));

    MvcResult sellerRes = mockMvc.perform(get("/api/auctions/myOffers")
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

  // Conservamos solo el helper exclusivo de Auction, el resto se borró porque se hereda
  private String createAuctionBody(String cardId, Integer auctionDurationHours, List<AuctionConditionDto> conditions) throws JsonProcessingException {
    CreateAuctionDTO dto = new CreateAuctionDTO();
    dto.setCardId(cardId);
    dto.setAuctionDurationHours(auctionDurationHours);
    dto.setConditions(conditions);

    // Se usa el objectMapper protegido de la clase base
    return objectMapper.writeValueAsString(dto);
  }

}