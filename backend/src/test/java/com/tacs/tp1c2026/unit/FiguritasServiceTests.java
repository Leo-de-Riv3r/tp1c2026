package com.tacs.tp1c2026.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import com.tacs.tp1c2026.services.FiguritasService;
import com.tacs.tp1c2026.unit.mothers.DtoMother;
import com.tacs.tp1c2026.unit.mothers.UsuarioMother;

import jakarta.transaction.Transactional;

@SpringBootTest
public class FiguritasServiceTests {

    private final UsuariosRepository usuariosRepository;
    private final FiguritasService figuritasService;
    private final DtoMother inputDtoFactory;

    private Usuario mockUsuario;

    public FiguritasServiceTests(FiguritasService figuritasService, DtoMother inputDtoFactory, UsuariosRepository usuariosRepository) {
        this.figuritasService = figuritasService;
        this.inputDtoFactory = inputDtoFactory;
        this.usuariosRepository = usuariosRepository;
    }

    @BeforeEach
    void setUp() {
        usuariosRepository.deleteAll();
        mockUsuario = UsuarioMother.builder().build();
        usuariosRepository.save(mockUsuario);
    }

    // ── Figuritas Repetidas (US1) ──────────────────────────────────────────────

    @Test
    @Transactional
    public void testPublicarFigurita() {
        FiguritaRepetidaDto dto = inputDtoFactory.createMockFiguritaRepetidaDto();
        figuritasService.registrarFiguritaRepetida(dto, mockUsuario.getId());
        Usuario usuarioActualizado = usuariosRepository.findById(mockUsuario.getId()).orElseThrow();
        assertEquals(1, usuarioActualizado.getRepetidas().size());
    }

    @Test
    @Transactional
    public void testPublicarFiguritaDosVecesLanzaExcepcion() {
        FiguritaRepetidaDto dto = inputDtoFactory.createMockFiguritaRepetidaDto();
        figuritasService.registrarFiguritaRepetida(dto, mockUsuario.getId());
        assertThrows(ConflictException.class, () -> figuritasService.registrarFiguritaRepetida(dto, mockUsuario.getId()));
    }

    @Test
    @Transactional
    public void testPublicarFiguritaConFiguritaRepetidaLanzaExcepcion() {
        FiguritaRepetidaDto dto = inputDtoFactory.createMockFiguritaRepetidaDto();
        figuritasService.registrarFiguritaRepetida(dto, mockUsuario.getId());
        assertThrows(ConflictException.class, () -> figuritasService.registrarFiguritaRepetida(dto, mockUsuario.getId()));
    }

    // ── Figuritas Faltantes (US2) ──────────────────────────────────────────────

    @Test
    @Transactional
    public void testPublicarFiguritaFaltante() {
        FiguritaFaltanteDto dto = inputDtoFactory.createMockFiguritaFaltanteDto();
        figuritasService.registrarFiguritaFaltante(dto, mockUsuario.getId());
        Usuario usuarioActualizado = usuariosRepository.findById(mockUsuario.getId()).orElseThrow();
        assertEquals(1, usuarioActualizado.getFaltantes().size());
    }

    @Test
    @Transactional
    public void testPublicarFiguritaFaltanteDosVecesLanzaExcepcion() {
        FiguritaFaltanteDto dto = inputDtoFactory.createMockFiguritaFaltanteDto();
        figuritasService.registrarFiguritaFaltante(dto, mockUsuario.getId());
        assertThrows(ConflictException.class, () -> figuritasService.registrarFiguritaFaltante(dto, mockUsuario.getId()));
    }

    @Test
    @Transactional
    public void testPublicarFiguritaFaltanteConFiguritaFaltanteLanzaExcepcion() {
        FiguritaFaltanteDto dto = inputDtoFactory.createMockFiguritaFaltanteDto();
        figuritasService.registrarFiguritaFaltante(dto, mockUsuario.getId());
        assertThrows(ConflictException.class, () -> figuritasService.registrarFiguritaFaltante(dto, mockUsuario.getId()));
    }

    @Test
    @Transactional
    public void testObtenerFigurasRepetidas() {
        Usuario usuario = UsuarioMother.builder().withRepetida().build();
        usuario = usuariosRepository.save(usuario);
        assertEquals(1, figuritasService.obtenerFiguritasRepetidas(usuario.getId()).size());
    }

    @Test
    @Transactional
    public void testObtenerFiguritasRepetidasSobreUsuarioNoExistenteLanzaExcepcion() {
        assertThrows(UserNotFoundException.class, () -> figuritasService.obtenerFiguritasRepetidas(999));
    }

    @Test
    @Transactional
    public void testObtenerFigurasFaltantes() {
        Usuario usuario = UsuarioMother.builder().withFaltante().build();
        usuario = usuariosRepository.save(usuario);
        assertEquals(1, figuritasService.obtenerFiguritasFaltantes(usuario.getId()).size());
    }

    @Test
    @Transactional
    public void testObtenerFiguritasFaltantesSobreUsuarioNoExistenteLanzaExcepcion() {
        assertThrows(UserNotFoundException.class, () -> figuritasService.obtenerFiguritasFaltantes(999));
    }

}
