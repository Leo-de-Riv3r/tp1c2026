package com.tacs.tp1c2026;

import com.tacs.tp1c2026.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthMongoTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsersRepository usersRepository;

    @BeforeEach
    void setUp() {
        usersRepository.deleteAll();
    }

    @Test
    void registerUsuarioDevuelveOkYUsuario() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("Nuevo User", "nuevo@test.com", "secreto123", "avatar-1")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.email").value("nuevo@test.com"));
    }

    @Test
    void loginUsuarioDevuelve200() throws Exception {
        registrarUsuario("User Login", "user@login.com", "clave123", "avatar-2");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("user@login.com", "clave123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value("user@login.com"));
    }

    @Test
    void adminLoginDevuelve200() throws Exception {
        mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("admin@test.com", "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.id").value("admin"))
            .andExpect(jsonPath("$.user.email").value("admin@test.com"));
    }

    @Test
    void usuarioIntentaLoguearseComoAdminYDaError() throws Exception {
        registrarUsuario("User Comun", "normal@test.com", "clave123", "avatar-3");

        mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("normal@test.com", "clave123")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminIntentaLoguearseComoUserYDaError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("admin@test.com", "admin123")))
            .andExpect(status().isUnauthorized());
    }

    private void registrarUsuario(String name, String email, String password, String avatarId) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(name, email, password, avatarId)))
            .andExpect(status().is2xxSuccessful());
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
