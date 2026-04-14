package com.tacs.tp1c2026;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; // <-- ESTE ES EL CORRECTO

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.entities.dto.input.PropuestaIntercambioDto;
import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import javax.print.attribute.standard.Media;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.PutMapping;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class Us1_3_5_10Tests {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UsuariosRepository usuariosRepository; // Repo real (H2)

  @Autowired
  private PublicacionesIntercambioRepository publicacionesIntercambioRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private Integer idUser1;
  private Integer idUser2;
  @BeforeEach
  void setUp() {
    usuariosRepository.deleteAll();
    Usuario usuario1 = new Usuario();
    usuario1.setNombre("user1");

    Usuario usuario2 = new Usuario();
    usuario2.setNombre("user2");

    idUser1 = usuariosRepository.save(usuario1).getId();
    idUser2 = usuariosRepository.save(usuario2).getId();
  }

  @Test
  @Transactional
  void registrarFigurita() throws Exception {
    // Preparar el DTO
    FiguritaRepetidaDto dto = new FiguritaRepetidaDto();
    dto.setNumero(10);
    dto.setJugador("Lionel Messi");
    dto.setSeleccion("Argentina");
    dto.setCategoria(Categoria.LEGENDARIO);
    dto.setDescripcion("El mejor jugador del mundo");
    dto.setCantidad(2);
    dto.setTipoParticipacion(TipoParticipacion.INTERCAMBIO);

    // Ejecutar POST usando el ID del usuario que creamos en el setUp
    mockMvc.perform(post("/api/figuritas/registrar-repetida")
            .param("userId", idUser1.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))

        .andExpect(status().isOk())
        .andExpect(content().string("Figurita repetida agregada"));

    // Verificación extra: ¿Realmente se guardó en la DB?
    Usuario usuarioPostTest = usuariosRepository.findById(idUser1).get();
    assertEquals(1, usuarioPostTest.getRepetidas().size());
  }

  @Test
  @Transactional
  void registrarFaltante() throws Exception {
    // Preparar el DTO
    FiguritaFaltanteDto dto = new FiguritaFaltanteDto();
    dto.setNumero(10);
    dto.setJugador("El diegote");
    dto.setEquipo("Napoli");
    dto.setCategoria(Categoria.LEGENDARIO);
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
  void ofrecerPropuestaIntercambioYAceptar() throws Exception {
    //setup all
    // Preparar el DTO
    FiguritaRepetidaDto dto = new FiguritaRepetidaDto();
    dto.setNumero(10);
    dto.setJugador("Lionel Messi");
    dto.setSeleccion("Argentina");
    dto.setCategoria(Categoria.LEGENDARIO);
    dto.setDescripcion("El mejor jugador del mundo");
    dto.setCantidad(2);
    dto.setTipoParticipacion(TipoParticipacion.INTERCAMBIO);

    // Ejecutar POST usando el ID del usuario que creamos en el setUp
    mockMvc.perform(post("/api/figuritas/registrar-repetida")
        .param("userId", idUser1.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)));

    //publicar figurita para intercambio
    mockMvc.perform(post("/api/publicaciones/intercambios")
        .param("userId", idUser1.toString())
        .param("numFigurita", "10"))
        .andExpect(status().isOk());

    //ofrecer propuesta
    mockMvc.perform(post("/api/figuritas/registrar-repetida")
        .param("userId", idUser2.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)));

    PropuestaIntercambioDto dtoPropuesta = new PropuestaIntercambioDto();
    dtoPropuesta.setNumfiguritas(List.of(10));

    mockMvc.perform(post("/api/publicaciones/intercambios/1/propuestas")
        .param("userId", idUser2.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dtoPropuesta)))
        .andExpect(status().isOk());

    mockMvc.perform(put("/api/publicaciones/intercambios/1/propuestas/1/aceptar")
        .param("userId", idUser1.toString()))
        .andExpect(status().isOk());
  }
}
