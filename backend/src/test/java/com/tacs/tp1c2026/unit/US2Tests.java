package com.tacs.tp1c2026.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.tacs.tp1c2026.controllers.FiguritasController;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import com.tacs.tp1c2026.unit.mothers.EntityMother;
import com.tacs.tp1c2026.unit.mothers.DtoMother;

import jakarta.transaction.Transactional;

@SpringBootTest
public class US2Tests {

    private final UsuariosRepository usuariosRepository;
    private final FiguritasController figuritasController;
    private final EntityMother entityFactory;
    private final DtoMother inputDtoFactory;

    private Usuario usuario;

    public US2Tests(UsuariosRepository usuariosRepository, FiguritasController figuritasController, EntityMother entityFactory, DtoMother inputDtoFactory) {
        this.usuariosRepository = usuariosRepository;
        this.figuritasController = figuritasController;
        this.entityFactory = entityFactory;
        this.inputDtoFactory = inputDtoFactory;
    }

    @BeforeEach
    void setUp() {
        usuariosRepository.deleteAll();
        usuario = entityFactory.user();
        usuariosRepository.save(usuario);
    }

    @Test
    @Transactional
    public void testPublicarFiguritaFaltante() {
        FiguritaFaltanteDto dto = inputDtoFactory.createMockFiguritaFaltanteDto();
        figuritasController.registrarFiguritaFaltante(dto, usuario.getId());
        Usuario usuarioActualizado = usuariosRepository.findById(usuario.getId()).orElseThrow();
        assertEquals(1, usuarioActualizado.getFaltantes().size());
    }

    @Test
    @Transactional
    public void testPublicarFiguritaFaltanteDosVeces() {
        FiguritaFaltanteDto dto = inputDtoFactory.createMockFiguritaFaltanteDto();
        figuritasController.registrarFiguritaFaltante(dto, usuario.getId());
        figuritasController.registrarFiguritaFaltante(dto, usuario.getId());
        Usuario usuarioActualizado = usuariosRepository.findById(usuario.getId()).orElseThrow();
        assertEquals(1, usuarioActualizado.getFaltantes().size());
    }
    
}
