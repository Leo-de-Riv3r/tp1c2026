package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Perfil;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.VectorProfile;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.properties.PerfilProperties;
import com.tacs.tp1c2026.repositories.PerfilRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuariosRepository usuariosRepository;
    private final PerfilProperties properties;

    public PerfilService(PerfilRepository perfilRepository, UsuariosRepository usuariosRepository, PerfilProperties properties) {
        this.perfilRepository = perfilRepository;
        this.usuariosRepository = usuariosRepository;
        this.properties = properties;
    }

    /**
    * Recalcula los perfiles a los que pertenece un usuario identificado por su ID.
    * Elimina al usuario de todos los perfiles actuales y lo asigna a los más cercanos
     * según su perfil vectorial.
     *
     * @param userId identificador del usuario
     * @throws UserNotFoundException si el usuario no existe
     */
    @Transactional
    public void actualizarPerfilesUsuario(Integer userId) {
        Usuario usuario = usuariosRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
        actualizarPerfiles(usuario);
    }

    /**
     * Recalcula los perfiles a los que pertenece el usuario dado.
     * Elimina al usuario de todos los perfiles y lo reasigna a los perfiles más cercanos
     * según su {@link com.tacs.tp1c2026.entities.VectorProfile}.
     *
     * @param usuario usuario cuya asignación de perfiles debe actualizarse
     */
    @Transactional
    public void actualizarPerfiles(Usuario usuario) {
        List<Perfil> perfils = getPerfiles();

        perfils.forEach(perfil -> perfil.removerVecino(usuario));

        int perfilesPorUsuario = Math.max(1, properties.getPerUser());
        List<Perfil> perfilesMasCercanos = obtenerPerfilesMasCercanos(perfils, usuario.getVectorProfile(), perfilesPorUsuario);
        for (Perfil perfil : perfilesMasCercanos) {
            perfil.agregarVecino(usuario);
        }

        perfilRepository.saveAll(perfils);
    }

    /**
    * Obtiene la lista de todos los perfiles, garantizando que se hayan inicializado
     * al menos la cantidad mínima configurada.
     *
     * @return lista de todos los {@link Perfil} existentes
     */
    @Transactional
    public List<Perfil> getPerfiles() {
        return this.perfilRepository.findAll();
    }

    /**
     * Retorna los {@code cantidadPerfiles} perfiles más cercanos al vector de perfil dado,
     * ordenados por afinidad descendente.
     *
     * @param vectorUsuario   perfil vectorial de referencia
     * @param cantidadPerfiles número de perfiles a retornar
     * @return lista de perfiles más cercanos al vector dado
     */
    @Transactional
    public List<Perfil> obtenerPerfilesMasCercanos(VectorProfile vectorUsuario, Integer cantidadPerfiles) {
        return obtenerPerfilesMasCercanos(getPerfiles(), vectorUsuario, cantidadPerfiles);
    }

    /**
     * Genera sugerencias de intercambio para todos los usuarios del sistema.
     * Para cada usuario calcula sus vecinos más complementarios, los ordena por afinidad
     * y los guarda como sugerencias. Finalmente actualiza los vectores representativos
      * de cada perfil basándose en sus vecinos actuales.
     */
    @Transactional
    public void generarSugerencias() {
          List<Perfil> perfils = getPerfiles();

        try (Stream<Usuario> usuarios = usuariosRepository.streamAll()) {
            usuarios.forEach(usuario -> {
                usuario.removerSugerencias();
                calcularNuevasSugerencias(usuario, perfils);
            });
        }

        actualizarVectoresPerfilesDesdeVecinos(perfils);
        perfilRepository.saveAll(perfils);
    }

    /**
     * Calcula y asigna nuevas sugerencias de intercambio al usuario dado.
     * Busca los perfils más complementarios al perfil del usuario, recopila candidatos
     * de sus vecinos, los ordena por afinidad y guarda los mejores resultados como sugerencias.
     *
     * @param usuario usuario para el que se calculan las sugerencias
     * @param perfils lista de todos los perfils disponibles
     */
    private void calcularNuevasSugerencias(Usuario usuario, List<Perfil> perfils) {
        int cantidadPerfiles = Math.max(1, properties.getSuggestionNearestPerfiles());
        int topVecinos = Math.max(1, properties.getSuggestionTopNeighbors());
        List<Perfil> perfilesMasCercanos = obtenerPerfilesMasComplementarios(perfils, usuario.getVectorProfile(), cantidadPerfiles);

        Map<Integer, Usuario> candidatos = new HashMap<>();
        for (Perfil perfil : perfilesMasCercanos) {
            perfil.getVecinos().stream()
                .filter(vecino -> !vecino.getId().equals(usuario.getId()))
                .forEach(vecino -> candidatos.putIfAbsent(vecino.getId(), vecino));
        }

        candidatos.values().stream()
            .sorted(Comparator.comparingInt((Usuario vecino) -> VectorProfile.complement(usuario.getVectorProfile(), vecino.getVectorProfile())).reversed())
            .limit(topVecinos)
            .forEach(usuario::agregarSugerencia);
    }

    /**
     * Retorna los {@code cantidadPerfiles} perfiles cuyo vector representativo es más
     * complementario al vector del usuario dado, ordenados por complementariedad descendente.
     *
     * @param perfils         lista completa de perfiles
     * @param vectorUsuario   vector de perfil del usuario
     * @param cantidadPerfiles cantidad de perfiles a retornar
     * @return sub-lista de los perfiles más complementarios
     */
    private List<Perfil> obtenerPerfilesMasComplementarios(List<Perfil> perfils, VectorProfile vectorUsuario, Integer cantidadPerfiles) {
        List<Perfil> sortedPerfils = new ArrayList<>(perfils);
        sortedPerfils.sort(Comparator.comparingInt((Perfil perfil) -> VectorProfile.complement(vectorUsuario, perfil.getVectorProfile())).reversed());
        return sortedPerfils.subList(0, Math.min(cantidadPerfiles, sortedPerfils.size()));
    }

    /**
    * Recalcula el vector representativo de cada perfil tomando los {@code kVecinos}
     * vecinos más cercanos y promediando sus perfiles vectoriales.
     *
     * @param perfils lista de perfils cuyos vectores se deben actualizar
     */
    private void actualizarVectoresPerfilesDesdeVecinos(List<Perfil> perfils) {
        int kVecinos = Math.max(1, properties.getVectorNearestNeighbors());

        for (Perfil perfil : perfils) {
            List<VectorProfile> vecinosMasCercanos = perfil.getVecinos().stream()
                .sorted(Comparator.comparingInt((Usuario vecino) -> VectorProfile.agreement(perfil.getVectorProfile(), vecino.getVectorProfile())).reversed())
                .limit(kVecinos)
                .map(Usuario::getVectorProfile)
                .toList();
            perfil.updateVector(vecinosMasCercanos);
        }
    }

    /**
     * Retorna los {@code cantidadPerfiles} perfiles más cercanos al vector dado según la
     * métrica de acuerdo ({@code agreement}) del perfil.
     *
     * @param perfils         lista completa de perfiles
     * @param vectorUsuario   vector de perfil de referencia
     * @param cantidadPerfiles cantidad máxima de perfiles a retornar
     * @return sub-lista de los perfiles más cercanos
     */
    private List<Perfil> obtenerPerfilesMasCercanos(List<Perfil> perfils, com.tacs.tp1c2026.entities.VectorProfile vectorUsuario, Integer cantidadPerfiles) {
        List<Perfil> sortedPerfils = new ArrayList<>(perfils);
        sortedPerfils.sort(Comparator.comparingInt((Perfil perfil) -> VectorProfile.agreement(perfil.getVectorProfile(), vectorUsuario)).reversed());
        return sortedPerfils.subList(0, Math.min(cantidadPerfiles, sortedPerfils.size()));
    }

    /**
    * Garantiza que exista la cantidad de perfiles configurada en {@link PerfilProperties#getProfiles()}.
     * Si hay menos perfiles de los requeridos, crea e inicializa los faltantes.
     *
     * @return lista completa de perfiles tras asegurar la cantidad mínima
     */
    @Transactional
    @PostConstruct
    public List<Perfil> ensurePerfilesInitialized() {
        List<Perfil> perfils = perfilRepository.findAll();

        int targetCount = Math.max(1, properties.getProfiles());
        if (perfils.size() < targetCount) {
            List<Perfil> faltantes = new ArrayList<>();
            for (int i = perfils.size(); i < targetCount; i++) {
                faltantes.add(new Perfil());
            }
            perfilRepository.saveAll(faltantes);
            perfils = perfilRepository.findAll();
        }

        return perfils;
    }
}
