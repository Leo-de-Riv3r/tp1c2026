package com.tacs.tp1c2026.unit;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaDto;
import com.tacs.tp1c2026.unit.mothers.DtoMother;
import com.tacs.tp1c2026.unit.mothers.FiguritaColeccionMother;
import com.tacs.tp1c2026.unit.mothers.FiguritaMother;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.repositories.SubastasRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import com.tacs.tp1c2026.services.AlertService;
import com.tacs.tp1c2026.services.SubastasService;
import com.tacs.tp1c2026.unit.mothers.UsuarioMother;

import jakarta.transaction.Transactional;

@SpringBootTest
public class SubastaServiceTests {

    private final UsuariosRepository usuariosRepository;
    private final AlertService alertService;
    private final SubastasRepository subastasRepository;
    private final SubastasService subastasService;

    public SubastaServiceTests(UsuariosRepository usuariosRepository, AlertService alertService, SubastasRepository subastasRepository, SubastasService subastasService) {
        this.usuariosRepository = usuariosRepository;
        this.alertService = alertService;
        this.subastasRepository = subastasRepository;
        this.subastasService = subastasService;
    }

    @Test
    @Transactional
    public void testCrearSubasta() {
        Figurita figurita = FiguritaMother.builder().build();
        FiguritaColeccion coleccion = FiguritaColeccionMother.builder().withFigurita(figurita).build()
        Usuario usuario = UsuarioMother.builder().withRepetida(coleccion).build();
        usuario = usuariosRepository.save(usuario);

        NuevaSubastaDto dto = NuevaSubastaDtoMother.builder().withFigurita(figurita).build();

        Integer id = subastasService.crearSubasta(usuario.getId(), dto);

        assert id != null;

    }

    @Test
    @Transactional
    public void testAgregarUsuarioInteresado() {


    }

    @Test
    @Transactional
    public void testOfertarSubasta() {

    }

    @Test
    @Transactional
    public void testOfertarSubastaCerradaLanzaExcepcion() {

    }

    @Test
    @Transactional
    public void testAceptarOfertaSubasta() {

    }

    @Test
    @Transactional
    public void testRechazarOfertaSubasta() {

    }



    

}