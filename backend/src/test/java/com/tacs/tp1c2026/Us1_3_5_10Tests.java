/*package com.tacs.tp1c2026;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; // <-- ESTE ES EL CORRECTO

import com.tacs.tp1c2026.entities.CardCollection;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.MissingCardDto;
import com.tacs.tp1c2026.entities.dto.input.RegisterRepeatedCardDto;
import com.tacs.tp1c2026.entities.dto.input.NewExchangeProposalDto;
import com.tacs.tp1c2026.entities.enums.CardCategory;
import com.tacs.tp1c2026.entities.enums.ParticipationType;
import com.tacs.tp1c2026.repositories.ExchangeProposalsRepository;
import com.tacs.tp1c2026.repositories.ExchangePublicationsRepository;
import com.tacs.tp1c2026.repositories.UsersRepository;
import jakarta.transaction.Transactional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class Us1_3_5_10Tests {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UsersRepository usuariosRepository; // Repo real (H2)

  @Autowired
  private ExchangePublicationsRepository publicacionesIntercambioRepository;

  @Autowired
  private ExchangeProposalsRepository propuestasIntercambioRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private Integer idUser1;
  private Integer idUser2;
  @BeforeEach
  void setUp() {
    usuariosRepository.deleteAll();
    Usuario usuario1 = new Usuario();
    usuario1.setName("user1");

    Usuario usuario2 = new Usuario();
    usuario2.setName("user2");

    idUser1 = usuariosRepository.save(usuario1).getId();
    idUser2 = usuariosRepository.save(usuario2).getId();
  }

  @Test
  @Transactional
  void registrarFigurita() throws Exception {
    // Preparar el DTO
    RegisterRepeatedCardDto dto = new RegisterRepeatedCardDto();
    dto.setNumero(10);
    dto.setJugador("Lionel Messi");
    dto.setSeleccion("Argentina");
    dto.setCardCategory(CardCategory.LEGENDARIO);
    dto.setDescripcion("El mejor jugador del mundo");
    dto.setCantidad(1);
    dto.setParticipationType(ParticipationType.INTERCAMBIO);

    // Ejecutar POST usando el ID del usuario que creamos en el setUp
    mockMvc.perform(post("/api/figuritas/registrar-repetida")
            .param("userId", idUser1.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))

        .andExpect(status().isOk())
        .andExpect(content().string("Card repetida agregada"));

    // Verificación extra: ¿Realmente se guardó en la DB?
    Usuario usuarioPostTest = usuariosRepository.findById(idUser1).get();
    assertEquals(1, usuarioPostTest.getRepetidas().size());
  }

  @Test
  @Transactional
  void registrarFaltante() throws Exception {
    // Preparar el DTO
    MissingCardDto dto = new MissingCardDto();
    dto.setNumero(10);
    dto.setJugador("El diegote");
    dto.setEquipo("Napoli");
    dto.setCardCategory(CardCategory.LEGENDARIO);
    dto.setDescripcion("El diego, el diegote, hasta los huevos diego");

    // Ejecutar POST
    mockMvc.perform(post("/api/figuritas/registrar-faltante")
        .param("userId", idUser1.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk());

    //verificacion de bd
    Usuario usuarioPostTest = usuariosRepository.findById(idUser1).get();
    assertEquals(1, usuarioPostTest.getFaltantes().size());
  }

  @Test
  @Transactional
  void ofrecerPropuestaIntercambioYAceptar() throws Exception {
    //setup all
    // Preparar el DTO
    RegisterRepeatedCardDto dto = new RegisterRepeatedCardDto();
    dto.setNumero(10);
    dto.setJugador("Lionel Messi");
    dto.setSeleccion("Argentina");
    dto.setCardCategory(CardCategory.LEGENDARIO);
    dto.setDescripcion("El mejor jugador del mundo");
    dto.setCantidad(1);
    dto.setParticipationType(ParticipationType.INTERCAMBIO);

    mockMvc.perform(post("/api/figuritas/registrar-repetida")
        .param("userId", idUser1.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)));

    mockMvc.perform(post("/api/publicaciones/intercambios")
        .param("userId", idUser1.toString())
        .param("numFigurita", "10"))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/figuritas/registrar-repetida")
        .param("userId", idUser2.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)));

    NewExchangeProposalDto dtoPropuesta = new NewExchangeProposalDto();
    dtoPropuesta.setNumfiguritas(List.of(10));

    mockMvc.perform(post("/api/publicaciones/intercambios/1/propuestas")
        .param("userId", idUser2.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dtoPropuesta)))
        .andExpect(status().isOk());
    //verificar card coleccion
    Usuario user2 = usuariosRepository.findById(2)
        .orElseThrow(() -> new RuntimeException("No se encontro el usuario"));
    CardCollection figuUsuario = user2.getRepetidas().get(0);
    assertEquals(1, figuUsuario.getCantidadOfertada());


    mockMvc.perform(put("/api/publicaciones/intercambios/1/propuestas/1/aceptar")
        .param("userId", idUser1.toString()))
        .andExpect(status().isOk());

    //verificar que el usuario ya no tiene card
    Usuario user2B = usuariosRepository.findById(idUser2)
        .orElseThrow(() -> new RuntimeException("No se encontro el usuario"));
    CardCollection figuOfrecidaUsuario = user2B.getRepetidas().get(0);
    assertEquals(0, figuOfrecidaUsuario.getCantidadLibre());
    assertEquals(0, figuOfrecidaUsuario.getCantidadOfertada());
  }
}
*/