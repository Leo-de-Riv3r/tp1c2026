package com.tacs.tp1c2026;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.auction.input.AuctionConditionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;

import com.tacs.tp1c2026.entities.enums.Category;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@SpringBootTest
@AutoConfigureMockMvc
public class AuctionTests {
  @Autowired
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired
  private CardRepository cardRepository;
  @Autowired
  private UserRepository userRepository;


  @BeforeEach
  void setup() throws IOException {
    userRepository.deleteAll();
    cardRepository.deleteAll();
    try (InputStream inputStream = getClass().getResourceAsStream("/catalog.json")) {
      List<Card> cards = objectMapper.readValue(inputStream, new TypeReference<List<Card>>() {});
      cardRepository.saveAll(cards);
      System.out.println("✅ Base de datos de prueba inicializada con " + cards.size() + " cartas.");
    }
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
