package com.tacs.tp1c2026;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.enums.ParticipationType;
import com.tacs.tp1c2026.repositories.CardsRepository;
import com.tacs.tp1c2026.repositories.UsersRepository;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
public class CardsCollectionTests {
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private CardsRepository cardsRepository;
  @Autowired
  private UsersRepository usersRepository;
  @BeforeEach
  void setUp() {
    usersRepository.deleteAll();
  }

  @Test
  void registerMissingCard() throws Exception {
    registrarUsuario("User Login", "user@login.com", "clave123", "avatar-2");
    String body = registerMissingBody(cardsRepository.findByNumber(1).get().getId());
    String token = getUserToken("user@login.com", "clave123");

    mockMvc.perform(post("/my-collection/missing")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .content(body))
        .andExpect(status().isOk());
  }

  @Test
  void registerRepeatedCard() throws  Exception{
    registrarUsuario("User Login", "user@login.com", "clave123", "avatar-2");
    String cardId = cardsRepository.findByNumber(1).get().getId();
    String body = registerRepeatedBody(cardId, 2, ParticipationType.SUBASTA);
    String token = getUserToken("user@login.com", "clave123");

    mockMvc.perform(post("/my-collection/repeated")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .content(body))
        .andExpect(status().isOk());
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

  private String registerMissingBody(String cardId) {
    return """
            {
              "cardId": "%s"
            }
            """.formatted(cardId);
  }

  private String registerRepeatedBody(String cardId, Integer quantity, ParticipationType participationType) {
    return """
            {
              "cardId": "%s",
              "quantity": %d,
              "participationType": "%s"
            }
            """.formatted(cardId, quantity, participationType);
  }
}
