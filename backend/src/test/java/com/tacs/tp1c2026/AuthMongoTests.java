package com.tacs.tp1c2026;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tacs.tp1c2026.entities.enums.UserRole;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.services.AuthService;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthMongoTests extends IntegrationTestBase {
  @Autowired
  private AuthService authService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void registerUsuarioDevuelveOkYUsuario() throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody("Nuevo User", "nuevo@test.com", "secreto123", "avatar-1")))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.email").value("nuevo@test.com"));
  }

  @Test
  void loginUsuarioDevuelveJwtConRoleUser() throws Exception {
    registrarUsuario("User Login", "user@login.com", "clave123", "avatar-2");

    MvcResult res = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody("user@login.com", "clave123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.user.email").value("user@login.com"))
        .andReturn();

    String token = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    assertEquals("USER", authService.extractRole(token));
  }

  @Test
  void loginAdminDevuelveJwtConRoleAdmin() throws Exception {
    registrarUsuario("Administrador", "admin@mail.com", "1234", "avatar-1");
    promoverAAdmin("admin@mail.com");

    MvcResult res = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody("admin@mail.com", "1234")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.user.email").value("admin@mail.com"))
        .andReturn();

    JsonNode body = objectMapper.readTree(res.getResponse().getContentAsString());
    String token = body.get("token").asText();
    assertEquals("ADMIN", authService.extractRole(token));
    // El user.id del admin es un ObjectId real (no el string mágico "admin" del flow viejo).
    assertEquals(24, body.get("user").get("id").asText().length());
  }

  @Test
  void loginConPasswordIncorrectoDa401() throws Exception {
    registrarUsuario("Administrador", "admin@mail.com", "1234", "avatar-1");
    promoverAAdmin("admin@mail.com");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody("admin@mail.com", "passwordIncorrecto")))
        .andExpect(status().isUnauthorized());
  }

  private void registrarUsuario(String name, String email, String password, String avatarId) throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody(name, email, password, avatarId)))
        .andExpect(status().is2xxSuccessful());
  }

  /** Promueve un user ya registrado a role=ADMIN escribiendo directo al repo (no hay endpoint). */
  private void promoverAAdmin(String email) {
    User u = userRepository.findByEmail(email).orElseThrow();
    u.setRole(UserRole.ADMIN);
    userRepository.save(u);
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
}
