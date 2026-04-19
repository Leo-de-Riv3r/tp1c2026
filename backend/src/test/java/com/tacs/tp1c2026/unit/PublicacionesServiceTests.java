package com.tacs.tp1c2026.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.PropuestasIntercambioRepository;
import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import com.tacs.tp1c2026.services.PublicacionesService;
import com.tacs.tp1c2026.unit.mothers.FiguritaMother;
import com.tacs.tp1c2026.unit.mothers.PublicacionIntercambioMother;
import com.tacs.tp1c2026.unit.mothers.UsuarioMother;

@SpringBootTest
public class PublicacionesServiceTests {

    private final PublicacionesService publicacionService;
    private final PublicacionesIntercambioRepository publicacionesIntercambioRepository;
    private final PropuestasIntercambioRepository propuestasIntercambioRepository;
    private final UsuariosRepository usuariosRepository;

    public PublicacionesServiceTests(PublicacionesService publicacionService, PublicacionesIntercambioRepository publicacionesIntercambioRepository, PropuestasIntercambioRepository propuestasIntercambioRepository, UsuariosRepository usuariosRepository) {
        this.publicacionService = publicacionService;
        this.publicacionesIntercambioRepository = publicacionesIntercambioRepository;
        this.propuestasIntercambioRepository = propuestasIntercambioRepository;
        this.usuariosRepository = usuariosRepository;
    }

    @BeforeEach
    public void setUp() {
        publicacionesIntercambioRepository.deleteAll();
        propuestasIntercambioRepository.deleteAll();
    }

    @Test
    public void testPublicarIntercambio() {

        Usuario usuario1 = UsuarioMother.builder().withRepetida().build();
        usuario1 = usuariosRepository.save(usuario1);

        publicacionService.publicarIntercambioFigurita(usuario1.getId(), usuario1.getRepetidas().get(0).getId());

        List<PublicacionIntercambio> publicaciones = publicacionesIntercambioRepository.findAll();
        assertEquals(1, publicaciones.size());
    }

    @Test
    public void testPublicarIntercambioSobreFiguritaQueNoTieneLanzaExcepcion() {
        final Usuario usuario = usuariosRepository.save(UsuarioMother.builder().build());

        assertThrows(NotFoundException.class, () -> publicacionService.publicarIntercambioFigurita(usuario.getId(), 999));
    }

    @Test
    public void testPublicarDosVecesSobreMismaFiguritaLanzaExcepcion() {
        final Usuario usuario = usuariosRepository.save(UsuarioMother.builder().withRepetida().build());
        Integer id = usuario.getRepetidas().get(0).getFigurita().getId();

        publicacionService.publicarIntercambioFigurita(usuario.getId(), id);

        assertThrows(ConflictException.class, () -> publicacionService.publicarIntercambioFigurita(usuario.getId(), id));
    }

    @Test
    public void testPublicarIntercambioQueNoSeTieneDuplicadosLanzaExcepcion() {
        Figurita figurita = FiguritaMother.builder().withId(1).withNumero(1).build();
        final Usuario usuario = usuariosRepository.save(UsuarioMother.builder()
            .withRepetidas(new FiguritaColeccion(0, TipoParticipacion.INTERCAMBIO, figurita))
            .build());

        assertThrows(ConflictException.class, () -> publicacionService.publicarIntercambioFigurita(usuario.getId(), figurita.getId()));
    }

    @Test
    public void testPublicarIntercambioMarcaFiguritaComoPublicada() {
        final Usuario usuario = usuariosRepository.save(UsuarioMother.builder().withRepetida().build());
        Integer id = usuario.getRepetidas().get(0).getFigurita().getId();

        publicacionService.publicarIntercambioFigurita(usuario.getId(), id);

        List<PublicacionIntercambio> publicaciones = publicacionesIntercambioRepository.findAll();
        assertEquals(1, publicaciones.size());
        assertTrue(publicaciones.get(0).getFiguritaColeccion().getPublicada());
    }

    @Test
    public void testCrearPropuestaIntercambio() {

        Usuario usuario1 = UsuarioMother.builder().withRepetida().build();
        Usuario usuario2 = UsuarioMother.builder().withRepetida().build();
        usuario1 = usuariosRepository.save(usuario1);
        usuario2 = usuariosRepository.save(usuario2);

        PublicacionIntercambio publicacion = PublicacionIntercambioMother.builder()
            .withPublicante(usuario1)
            .withFiguritaColeccion(usuario1.getRepetidas().get(0))
            .build();

        PublicacionIntercambio publicacionGuardada = publicacionesIntercambioRepository.save(publicacion);

        publicacionService.ofrecerPropuestaIntercambio(usuario2.getId(), publicacionGuardada.getId(), List.of(usuario2.getRepetidas().get(0).getFigurita().getId()));

        assertEquals(1, propuestasIntercambioRepository.findAll().size());
    }

}
