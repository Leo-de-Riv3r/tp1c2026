package com.tacs.tp1c2026;

import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de migración a MongoDB. Requieren una instancia de Mongo corriendo
 * (ver `application.properties` de tests).
 */
@SpringBootTest
@AutoConfigureMockMvc
public class UsersMongoTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  private String userId;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    User user = new User();
    user.setName("Test User");
    user.setEmail("test@test.com");
    userId = userRepository.save(user).getId();
  }

  @Test
  void getUsuarioPorIdDevuelve200() throws Exception {
    mockMvc.perform(get("/api/users/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test User"))
        .andExpect(jsonPath("$.email").value("test@test.com"));
  }

  @Test
  void getCollectionDeUsuarioNuevoEsVacia() throws Exception {
    mockMvc.perform(get("/api/users/" + userId + "/collection"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }
}
