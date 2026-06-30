package com.tacs.tp1c2026;

import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ValidationErrorShapeTests extends IntegrationTestBase {

    /**
     * POST /api/auth/login con body vacío: LoginDTO tiene @NotBlank en email y password,
     * así que dispara MethodArgumentNotValidException con dos field errors.
     */
    @Test
    void validationErrorReturnsApiErrorShape() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message", containsString("Datos inválidos")))
            .andExpect(jsonPath("$.message", containsString("email")))
            .andExpect(jsonPath("$.message", containsString("password")))
            .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
