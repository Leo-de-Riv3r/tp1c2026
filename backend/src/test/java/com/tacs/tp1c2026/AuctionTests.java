package com.tacs.tp1c2026;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.dto.auction.input.AuctionConditionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class AuctionTests {
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Test
  void publishCardForAuction() throws Exception {
    registrarUsuario("testUser", "test@java.com", "password123", "avatar1");
    String token = getUserToken("test@java.com", "password123");
    String cardId = "card_021";
    //pongo id de user aleatorio porque lo extrae del token
    registerRepeatedCard(cardId, token, "1");

    //create auction

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

  private String createAuctionBody(String cardId, Integer auctionDurationHours, List<AuctionConditionDto> conditions){
    CreateAuctionDTO dto = new CreateAuctionDTO();
    dto.setCardId(cardId);
    dto.setAuctionDurationHours(auctionDurationHours);
    dto.setConditions(conditions);

    return objectMapper.writeValueAsString(dto);
  }

}
