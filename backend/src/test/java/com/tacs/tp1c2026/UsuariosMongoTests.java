package com.tacs.tp1c2026;

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de ejemplo para la migración a MongoDB.
 * Usan MongoDB embebido (flapdoodle) — no requieren base local levantada.
 *
 * Diferencias respecto a los tests JPA anteriores (Us1_3_5_10Tests):
 * - El id del usuario es String (ObjectId) en lugar de Integer
 * - No hay @Transactional — MongoDB embebido no lo necesita
 * - Los repositorios son MongoRepository en lugar de JpaRepository
 * - setUp() usa el builder de Lombok en lugar de setters
 */

@SpringBootTest
@AutoConfigureMockMvc
public class UsuariosMongoTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsersRepository usersRepository;

    private String userId;

    @BeforeEach
    void setUp() {
        usersRepository.deleteAll();
        userId = usersRepository.save(
            Usuario.builder()
                .name("Test User")
                .email("test@test.com")
                .build()
        ).getId();
    }

    /**
     * GET simple — verifica que el usuario creado en setUp es accesible por su ID.
     * El id ahora es String (ObjectId de MongoDB), no Integer.
     */
    @Test
    void getUsuarioPorIdDevuelve200() throws Exception {
        mockMvc.perform(get("/users/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test User"))
            .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    /**
     * GET con subdocumento embebido — nuevo patrón que no existía en JPA.
     * Verifica que la colección del usuario es un array vacío al crearse.
     * En JPA esto requería una tabla separada con JOIN — acá es un campo del documento.
     */
    @Test
    void getCollectionDeUsuarioNuevoEsVacia() throws Exception {
        mockMvc.perform(get("/users/" + userId + "/collection"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }
}
