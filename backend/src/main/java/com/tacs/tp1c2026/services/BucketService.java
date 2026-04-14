package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.VectorProfile;
import com.tacs.tp1c2026.entities.bucket.Bucket;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.properties.BucketProperties;
import com.tacs.tp1c2026.repositories.BucketRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BucketService {

    private final BucketRepository bucketRepository;
    private final UsuariosRepository usuariosRepository;
    private final BucketProperties properties;

    public BucketService(BucketRepository bucketRepository, UsuariosRepository usuariosRepository, BucketProperties properties) {
        this.bucketRepository = bucketRepository;
        this.usuariosRepository = usuariosRepository;
        this.properties = properties;
    }

    /**
     * Recalcula los buckets a los que pertenece un usuario identificado por su ID.
     * Elimina al usuario de todos los buckets actuales y lo asigna a los más cercanos
     * según su perfil vectorial.
     *
     * @param userId identificador del usuario
     * @throws UserNotFoundException si el usuario no existe
     */
    @Transactional
    public void actualizarBucketsUsuario(Integer userId) {
        Usuario usuario = usuariosRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
        actualizarBuckets(usuario);
    }

    /**
     * Recalcula los buckets a los que pertenece el usuario dado.
     * Elimina al usuario de todos los buckets y lo reasigna a los buckets más cercanos
     * según su {@link com.tacs.tp1c2026.entities.VectorProfile}.
     *
     * @param usuario usuario cuya asignación de buckets debe actualizarse
     */
    @Transactional
    public void actualizarBuckets(Usuario usuario) {
        List<Bucket> buckets = getBuckets();

        buckets.forEach(bucket -> bucket.removerVecino(usuario));

        int bucketsPorUsuario = Math.max(1, properties.getPerUser());
        List<Bucket> bucketsMasCercanos = obtenerBucketsMasCercanos(buckets, usuario.getVectorProfile(), bucketsPorUsuario);
        for (Bucket bucket : bucketsMasCercanos) {
            bucket.agregarVecino(usuario);
        }

        bucketRepository.saveAll(buckets);
    }

    /**
     * Obtiene la lista de todos los buckets, garantizando que se hayan inicializado
     * al menos la cantidad mínima configurada.
     *
     * @return lista de todos los {@link com.tacs.tp1c2026.entities.bucket.Bucket} existentes
     */
    @Transactional
    public List<Bucket> getBuckets() {
        return ensureBucketsInitialized();
    }

    /**
     * Retorna los {@code cantidadBuckets} buckets más cercanos al vector de perfil dado,
     * ordenados por afinidad descendente.
     *
     * @param vectorUsuario   perfil vectorial de referencia
     * @param cantidadBuckets número de buckets a retornar
     * @return lista de buckets más cercanos al vector dado
     */
    @Transactional
    public List<Bucket> obtenerBucketsMasCercanos(VectorProfile vectorUsuario, Integer cantidadBuckets) {
        return obtenerBucketsMasCercanos(getBuckets(), vectorUsuario, cantidadBuckets);
    }

    /**
     * Genera sugerencias de intercambio para todos los usuarios del sistema.
     * Para cada usuario calcula sus vecinos más complementarios, los ordena por afinidad
     * y los guarda como sugerencias. Finalmente actualiza los vectores representativos
     * de cada bucket basándose en sus vecinos actuales.
     */
    @Transactional
    public void generarSugerencias() {
        List<Bucket> buckets = getBuckets();

        try (Stream<Usuario> usuarios = usuariosRepository.streamAll()) {
            usuarios.forEach(usuario -> {
                usuario.removerSugerencias();
                calcularNuevasSugerencias(usuario, buckets);
            });
        }

        actualizarVectoresBucketsDesdeVecinos(buckets);
        bucketRepository.saveAll(buckets);
    }

    /**
     * Calcula y asigna nuevas sugerencias de intercambio al usuario dado.
     * Busca los buckets más complementarios al perfil del usuario, recopila candidatos
     * de sus vecinos, los ordena por afinidad y guarda los mejores resultados como sugerencias.
     *
     * @param usuario usuario para el que se calculan las sugerencias
     * @param buckets lista de todos los buckets disponibles
     */
    private void calcularNuevasSugerencias(Usuario usuario, List<Bucket> buckets) {
        int cantidadBuckets = Math.max(1, properties.getSuggestionNearestBuckets());
        int topVecinos = Math.max(1, properties.getSuggestionTopNeighbors());
        List<Bucket> bucketsMasCercanos = obtenerBucketsMasComplementarios(buckets, usuario.getVectorProfile(), cantidadBuckets);

        Map<Integer, Usuario> candidatos = new HashMap<>();
        for (Bucket bucket : bucketsMasCercanos) {
            bucket.getVecinos().stream()
                .filter(vecino -> !vecino.getId().equals(usuario.getId()))
                .forEach(vecino -> candidatos.putIfAbsent(vecino.getId(), vecino));
        }

        candidatos.values().stream()
            .sorted(Comparator.comparingInt((Usuario vecino) -> calcularAfinidad(usuario, vecino)).reversed())
            .limit(topVecinos)
            .forEach(usuario::agregarSugerencia);
    }

    /**
     * Calcula la afinidad de complementariedad entre dos usuarios comparando sus perfiles vectoriales.
     *
     * @param usuario usuario base
     * @param vecino  usuario candidato a sugerencia
     * @return puntaje de afinidad (mayor es mejor)
     */
    private int calcularAfinidad(Usuario usuario, Usuario vecino) {
        return usuario.getVectorProfile().complement(vecino.getVectorProfile());
    }

    /**
     * Retorna los {@code cantidadBuckets} buckets cuyo vector representativo es más
     * complementario al vector del usuario dado, ordenados por complementariedad descendente.
     *
     * @param buckets         lista completa de buckets
     * @param vectorUsuario   vector de perfil del usuario
     * @param cantidadBuckets cantidad de buckets a retornar
     * @return sub-lista de los buckets más complementarios
     */
    private List<Bucket> obtenerBucketsMasComplementarios(List<Bucket> buckets, VectorProfile vectorUsuario, Integer cantidadBuckets) {
        List<Bucket> sortedBuckets = new ArrayList<>(buckets);
        sortedBuckets.sort(Comparator.comparingInt((Bucket bucket) -> vectorUsuario.complement(bucket.getVectorProfile())).reversed());
        return sortedBuckets.subList(0, Math.min(cantidadBuckets, sortedBuckets.size()));
    }

    /**
     * Recalcula el vector representativo de cada bucket tomando los {@code kVecinos}
     * vecinos más cercanos y promediando sus perfiles vectoriales.
     *
     * @param buckets lista de buckets cuyos vectores se deben actualizar
     */
    private void actualizarVectoresBucketsDesdeVecinos(List<Bucket> buckets) {
        int kVecinos = Math.max(1, properties.getVectorNearestNeighbors());

        for (Bucket bucket : buckets) {
            List<VectorProfile> vecinosMasCercanos = bucket.getVecinos().stream()
                .sorted(Comparator.comparingInt((Usuario vecino) -> bucket.calcularAfinidad(vecino.getVectorProfile())).reversed())
                .limit(kVecinos)
                .map(Usuario::getVectorProfile)
                .toList();
            bucket.updateVector(vecinosMasCercanos);
        }
    }

    /**
     * Retorna los {@code cantidadBuckets} buckets más cercanos al vector dado según la
     * métrica de acuerdo ({@code agreement}) del bucket.
     *
     * @param buckets         lista completa de buckets
     * @param vectorUsuario   vector de perfil de referencia
     * @param cantidadBuckets cantidad máxima de buckets a retornar
     * @return sub-lista de los buckets más cercanos
     */
    private List<Bucket> obtenerBucketsMasCercanos(List<Bucket> buckets, com.tacs.tp1c2026.entities.VectorProfile vectorUsuario, Integer cantidadBuckets) {
        List<Bucket> sortedBuckets = new ArrayList<>(buckets);
        sortedBuckets.sort(Comparator.comparingInt((Bucket bucket) -> bucket.calcularAfinidad(vectorUsuario)).reversed());
        return sortedBuckets.subList(0, Math.min(cantidadBuckets, sortedBuckets.size()));
    }

    /**
     * Garantiza que exista la cantidad de buckets configurada en {@link BucketProperties#getCount()}.
     * Si hay menos buckets de los requeridos, crea e inicializa los faltantes.
     *
     * @return lista completa de buckets tras asegurar la cantidad mínima
     */
    @Transactional
    protected List<Bucket> ensureBucketsInitialized() {
        List<Bucket> buckets = bucketRepository.findAll();

        int targetCount = Math.max(1, properties.getCount());
        if (buckets.size() < targetCount) {
            List<Bucket> faltantes = new ArrayList<>();
            for (int i = buckets.size(); i < targetCount; i++) {
                faltantes.add(new Bucket());
            }
            bucketRepository.saveAll(faltantes);
            buckets = bucketRepository.findAll();
        }

        return buckets;
    }
}
