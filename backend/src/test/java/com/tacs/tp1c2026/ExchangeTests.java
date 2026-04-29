package com.tacs.tp1c2026;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.entities.dto.input.NewExchangeProposalDto;
import com.tacs.tp1c2026.entities.dto.input.ReviewProposalDto;
import com.tacs.tp1c2026.entities.enums.ParticipationType;
import com.tacs.tp1c2026.entities.enums.ReviewAction;
import com.tacs.tp1c2026.repositories.CardsRepository;
import com.tacs.tp1c2026.repositories.ExchangeProposalsRepository;
import com.tacs.tp1c2026.repositories.ExchangePublicationsRepository;
import com.tacs.tp1c2026.repositories.UsersRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
public class ExchangeTests {
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private UsersRepository usersRepository;
  @Autowired
  private CardsRepository  cardsRepository;
  @Autowired
  private ExchangePublicationsRepository exchangePublicationsRepository;
  @Autowired
  private ExchangeProposalsRepository exchangeProposalsRepository;
  @Autowired
  private ObjectMapper objectMapper = new ObjectMapper();
  @BeforeEach
  void setUp() {
    exchangeProposalsRepository.deleteAll();
    exchangePublicationsRepository.deleteAll();
    usersRepository.deleteAll();
  }

  @Test
  void publishCardForExchange() throws Exception {
    registrarUsuario("user1", "user1@example.com", "password", "avatar1");
    String token = getUserToken("user1@example.com", "password");
    String cardId = cardsRepository.findByNumber(1).get().getId();

    registerRepeated(cardId, 1, ParticipationType.INTERCAMBIO, token);

    mockMvc.perform(post("/exchanges/")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(publishExchangeBody(cardId, 1)))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void offerExchangeProposal() throws Exception {
    registrarUsuario("user1", "user1@example.com", "password", "avatar1");
    String tokenUser1 = getUserToken("user1@example.com", "password");
    String cardPublicadaId = cardsRepository.findByNumber(1).get().getId();

    registerRepeated(cardPublicadaId, 1, ParticipationType.INTERCAMBIO, tokenUser1);

    mockMvc.perform(post("/exchanges/")
            .header("Authorization", "Bearer " + tokenUser1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(publishExchangeBody(cardPublicadaId, 1)))
        .andExpect(status().is2xxSuccessful());

    String publicationId = exchangePublicationsRepository.findAll().get(0).getId();

    registrarUsuario("user2", "user2@example.com", "password", "avatar2");
    String tokenUser2 = getUserToken("user2@example.com", "password");
    String cardOfertadaId = cardsRepository.findByNumber(8).get().getId();

    registerRepeated(cardOfertadaId, 1, ParticipationType.INTERCAMBIO, tokenUser2);

    mockMvc.perform(post("/exchanges/" + publicationId + "/proposals")
        .header("Authorization", "Bearer " + tokenUser2)
        .contentType(MediaType.APPLICATION_JSON)
        .content(publishExchangeProposalBody(List.of(cardOfertadaId)))
    ).andExpect(status().isOk());
  }

  @Test
  void AcceptExchangeProposal() throws Exception{
    registrarUsuario("user1", "user1@example.com", "password", "avatar1");
    String tokenUser1 = getUserToken("user1@example.com", "password");
    String cardPublicadaId = cardsRepository.findByNumber(1).get().getId();

    registerRepeated(cardPublicadaId, 1, ParticipationType.INTERCAMBIO, tokenUser1);

    mockMvc.perform(post("/exchanges/")
            .header("Authorization", "Bearer " + tokenUser1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(publishExchangeBody(cardPublicadaId, 1)))
        .andExpect(status().is2xxSuccessful());

    String publicationId = exchangePublicationsRepository.findAll().get(0).getId();

    registrarUsuario("user2", "user2@example.com", "password", "avatar2");
    String tokenUser2 = getUserToken("user2@example.com", "password");
    String cardOfertadaId = cardsRepository.findByNumber(8).get().getId();

    registerRepeated(cardOfertadaId, 1, ParticipationType.INTERCAMBIO, tokenUser2);

    mockMvc.perform(post("/exchanges/" + publicationId + "/proposals")
        .header("Authorization", "Bearer " + tokenUser2)
        .contentType(MediaType.APPLICATION_JSON)
        .content(publishExchangeProposalBody(List.of(cardOfertadaId)))
    ).andExpect(status().isOk());

    String proposalId = exchangeProposalsRepository.findAll().get(0).getId();

    mockMvc.perform(put("/exchanges/" + publicationId + "/proposals/" + proposalId)
        .header("Authorization", "Bearer " + tokenUser1)
        .contentType(MediaType.APPLICATION_JSON)
        .content(reviewProposalBody(ReviewAction.ACCEPT))
    ).andExpect(status().isOk());
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



  private void registerRepeated(String cardId, Integer quantity, ParticipationType participationType, String token) throws Exception {
    mockMvc.perform(post("/my-collection/repeated")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerRepeatedBody(cardId, quantity, participationType)))
        .andExpect(status().is2xxSuccessful());
  }

  private String reviewProposalBody(ReviewAction action){
    ReviewProposalDto dto  = new ReviewProposalDto();
    dto.setAction(action);
    return objectMapper.writeValueAsString(dto);
  }

    private String publishExchangeProposalBody(List<String> cardsIds) throws Exception {
      NewExchangeProposalDto dto = new NewExchangeProposalDto();
      dto.setCardsIds(cardsIds);

      return objectMapper.writeValueAsString(dto);
  }

  private String publishExchangeBody(String cardId, Integer quantity) {
    return """ 
            {
              "cardId": "%s",
              "quantity": %d
            }
            """.formatted(cardId, quantity);
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
