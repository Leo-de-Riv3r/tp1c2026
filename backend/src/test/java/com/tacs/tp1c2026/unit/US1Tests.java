package com.tacs.tp1c2026.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.tacs.tp1c2026.controllers.FiguritasController;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import com.tacs.tp1c2026.unit.mothers.EntityMother;
import com.tacs.tp1c2026.unit.mothers.DtoMother;

import jakarta.transaction.Transactional;


@SpringBootTest
public class US1Tests {


    private EntityMother mockFactory;
    private UsuariosRepository usuariosRepository;

    private Usuario mockUsuario1;

    private DtoMother inputDtoFactory;
    private FiguritasController figuritasController;

    public US1Tests(FiguritasController figuritasController, EntityMother entityFactory, 
        DtoMother inputDtoFactory,  UsuariosRepository usuariosRepository) {
        this.mockFactory = entityFactory;
        this.inputDtoFactory = inputDtoFactory;
        this.usuariosRepository = usuariosRepository;
    }

    @BeforeEach
    void setUp() {
       usuariosRepository.deleteAll();
       mockUsuario1 = mockFactory.user();
       usuariosRepository.save(mockUsuario1);
    }

    @Test
    @Transactional
    public void testPublicarFigurita() {
        FiguritaRepetidaDto dto = inputDtoFactory.createMockFiguritaRepetidaDto();
        figuritasController.agregarFigurita(dto, mockUsuario1.getId());
        Usuario usuarioActualizado = usuariosRepository.findById(mockUsuario1.getId()).orElseThrow();
        assertEquals(usuarioActualizado.getRepetidas().size(),1);
    }

    @Test
    @Transactional
    public void testPublicarFiguritaDosVeces() {
        FiguritaRepetidaDto dto = inputDtoFactory.createMockFiguritaRepetidaDto();
        figuritasController.agregarFigurita(dto, mockUsuario1.getId());
        Usuario usuarioActualizado = usuariosRepository.findById(mockUsuario1.getId()).orElseThrow();
        assertEquals(usuarioActualizado.getRepetidas().size(),1);
    }

}
