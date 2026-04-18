package com.tacs.tp1c2026.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.Perfil;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.VectorProfile;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.properties.PerfilProperties;
import com.tacs.tp1c2026.repositories.PerfilRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import com.tacs.tp1c2026.services.PerfilService;
import com.tacs.tp1c2026.unit.mothers.EntityMother;

@SpringBootTest
public class US4Tests {

    private final EntityMother entityMother;
    private final PerfilService perfilService;
    private final PerfilProperties perfilProperties;
    private final PerfilRepository perfilRepository;
    private final UsuariosRepository usuariosRepository;
    
    public US4Tests (EntityMother entityMother, PerfilRepository perfilRepository, UsuariosRepository usuariosRepository, PerfilProperties perfilProperties) {
        this.entityMother = entityMother;
        this.perfilRepository = perfilRepository;
        this.usuariosRepository = usuariosRepository;
        this.perfilProperties = perfilProperties;
        this.perfilService = new PerfilService(perfilRepository, usuariosRepository, perfilProperties);
    }

    @BeforeEach
    public void setUp() {
        usuariosRepository.deleteAll();
        perfilRepository.deleteAll();
        perfilProperties.setProfiles(10);
        perfilService.ensurePerfilesInitialized();
    }

    @Test
    public void testPerfilesInicializados() {
        this.perfilProperties.setProfiles(10);
        this.perfilService.ensurePerfilesInitialized();
        assertEquals(10,perfilService.getPerfiles().size());
    }

    @Test
    @Transactional
    public void testEnsurePerfilesInitialized() {
        perfilRepository.deleteAll();
        perfilProperties.setProfiles(2);
        perfilService.ensurePerfilesInitialized();

        Perfil perfil1 = new Perfil();
        Perfil perfil2 = new Perfil();

        VectorProfile vector1 = new VectorProfile(java.util.Map.of(1, 1, 2, -1));
        VectorProfile vector2 = new VectorProfile(java.util.Map.of(1, -1, 2, 1));

        perfil1.updateVector(java.util.List.of(vector1));
        perfil2.updateVector(java.util.List.of(vector2));

        perfilRepository.saveAll(java.util.List.of(perfil1, perfil2));

        List<Perfil> perfiles = perfilService.ensurePerfilesInitialized();

        assertEquals(2, perfiles.size());
    }

    @Test
    public void testActualizarPerfilesUsuario() {
    
        Usuario usuario = entityMother.user();
        FiguritaColeccion figurita1 = entityMother.figuritaColeccion();
        Figurita figurita2 = entityMother.figurita();
        
        usuario.agregarFaltantes(figurita2);
        usuario.agregarRepetidas(figurita1);
 
        assertTrue(perfilService.getPerfiles().stream()
            .filter(perfil -> perfil.getVecinos().contains(usuario))
            .count() > 0);

    }

    @Test
    public void testVectorProfilesComplementCards() {

        Usuario usuario1 = entityMother.user();
        Usuario usuario2 = entityMother.user();

        Figurita figurita1 = entityMother.figurita();
        FiguritaColeccion figurita2 = entityMother.figuritaColeccion();

        usuario1.agregarFaltantes(figurita1);
        usuario1.agregarRepetidas(figurita2);

        usuario2.agregarFaltantes(figurita2.getFigurita());
        usuario2.agregarRepetidas(new FiguritaColeccion(1,TipoParticipacion.INTERCAMBIO, figurita1));

        assertEquals(2, VectorProfile.complement(usuario1.getVectorProfile(), usuario2.getVectorProfile()));


    }

    @Test
    public void testVectorProfilesSimilaritySameMissingCard() {

        Usuario usuario1 = entityMother.user();
        Usuario usuario2 = entityMother.user();

        Figurita figurita1 = entityMother.figurita();
        FiguritaColeccion figurita2 = entityMother.figuritaColeccion();

        usuario1.agregarFaltantes(figurita1);
        usuario1.agregarRepetidas(figurita2);

        usuario2.agregarFaltantes(figurita1);
        usuario2.agregarRepetidas(figurita2);

        assertEquals(1, VectorProfile.agreement(usuario1.getVectorProfile(), usuario2.getVectorProfile()));
    }

    @Test
    @Transactional
    public void testGenerarSugerenciasUsesComplementaryProfileNeighbors() {
        perfilProperties.setSuggestionNearestPerfiles(2);
        perfilProperties.setSuggestionTopNeighbors(1);

        Usuario usuario1 = createComplementaryUser(1, 2, "Usuario 1");
        Usuario usuario2 = createComplementaryUser(2, 1, "Usuario 2");

        Usuario savedUsuario1 = usuariosRepository.save(usuario1);
        Usuario savedUsuario2 = usuariosRepository.save(usuario2);

        Perfil perfil1 = createPerfilWithNeighbor(savedUsuario1);
        Perfil perfil2 = createPerfilWithNeighbor(savedUsuario2);

        perfilRepository.saveAll(java.util.List.of(perfil1, perfil2));

        perfilService.generarSugerencias();

        Usuario refreshedUsuario1 = usuariosRepository.findById(savedUsuario1.getId()).orElseThrow();
        Usuario refreshedUsuario2 = usuariosRepository.findById(savedUsuario2.getId()).orElseThrow();

        assertEquals(1, refreshedUsuario1.getSugerenciasIntercambios().size());
        assertEquals(savedUsuario2.getId(), refreshedUsuario1.getSugerenciasIntercambios().get(0).getId());
        assertEquals(1, refreshedUsuario2.getSugerenciasIntercambios().size());
        assertEquals(savedUsuario1.getId(), refreshedUsuario2.getSugerenciasIntercambios().get(0).getId());
    }

    @Test
    public void testPerfilUpdateVectorFromVecinosAverageSign() {

        Usuario vecino1 = entityMother.user();
        Usuario vecino2 = entityMother.user();

        Figurita figurita1 = entityMother.figurita();
        figurita1.setId(1);
        Figurita figurita2 = entityMother.figurita();
        figurita2.setId(2);

        vecino1.agregarFaltantes(figurita1);
        vecino1.agregarRepetidas(new FiguritaColeccion(1, TipoParticipacion.INTERCAMBIO, figurita2));

        vecino2.agregarFaltantes(figurita1);
        vecino2.agregarRepetidas(new FiguritaColeccion(1, TipoParticipacion.INTERCAMBIO, figurita2));

        Perfil perfil = new Perfil();
        perfil.agregarVecino(vecino1);
        perfil.agregarVecino(vecino2);

        perfil.updateVector(java.util.List.of(vecino1.getVectorProfile(), vecino2.getVectorProfile()));

        assertEquals("1:1;2:-1", perfil.getVectorProfile().serialize());
    }

    @Test
    public void testVectorProfilesSameCardsReturnsZero() {

        Usuario usuario1 = entityMother.user();
        Usuario usuario2 = entityMother.user();

        Figurita figurita1 = entityMother.figurita();
        FiguritaColeccion figurita2 = entityMother.figuritaColeccion();

        usuario1.agregarFaltantes(figurita1);
        usuario1.agregarRepetidas(figurita2);

        usuario2.agregarFaltantes(figurita1);
        usuario2.agregarRepetidas(figurita2);

        assertEquals(0, VectorProfile.complement(usuario1.getVectorProfile(), usuario2.getVectorProfile()));
    }

    @Test
    public void testActualizarPerfiles() {



    }

    private Usuario createComplementaryUser(int faltanteId, int repetidaId, String nombre) {
        Usuario usuario = entityMother.user();
        usuario.setId(null);
        usuario.setNombre(nombre);

        Figurita faltante = entityMother.figurita();
        faltante.setId(faltanteId);

        Figurita repetidaFigurita = entityMother.figurita();
        repetidaFigurita.setId(repetidaId);

        usuario.agregarFaltantes(faltante);
        usuario.agregarRepetidas(new FiguritaColeccion(1, TipoParticipacion.INTERCAMBIO, repetidaFigurita));

        return usuario;
    }

    private Perfil createPerfilWithNeighbor(Usuario vecino) {
        Perfil perfil = new Perfil();
        perfil.agregarVecino(vecino);
        perfil.updateVector(java.util.List.of(vecino.getVectorProfile()));
        return perfil;
    }

}
