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

    @Transactional
    public void actualizarBucketsUsuario(Integer userId) {
        Usuario usuario = usuariosRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
        actualizarBuckets(usuario);
    }

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

    @Transactional
    public List<Bucket> getBuckets() {
        return ensureBucketsInitialized();
    }

    @Transactional
    public List<Bucket> obtenerBucketsMasCercanos(VectorProfile vectorUsuario, Integer cantidadBuckets) {
        return obtenerBucketsMasCercanos(getBuckets(), vectorUsuario, cantidadBuckets);
    }

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

    private int calcularAfinidad(Usuario usuario, Usuario vecino) {
        return usuario.getVectorProfile().complement(vecino.getVectorProfile());
    }

    private List<Bucket> obtenerBucketsMasComplementarios(List<Bucket> buckets, VectorProfile vectorUsuario, Integer cantidadBuckets) {
        List<Bucket> sortedBuckets = new ArrayList<>(buckets);
        sortedBuckets.sort(Comparator.comparingInt((Bucket bucket) -> vectorUsuario.complement(bucket.getVectorProfile())).reversed());
        return sortedBuckets.subList(0, Math.min(cantidadBuckets, sortedBuckets.size()));
    }

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

    private List<Bucket> obtenerBucketsMasCercanos(List<Bucket> buckets, com.tacs.tp1c2026.entities.VectorProfile vectorUsuario, Integer cantidadBuckets) {
        List<Bucket> sortedBuckets = new ArrayList<>(buckets);
        sortedBuckets.sort(Comparator.comparingInt((Bucket bucket) -> bucket.calcularScoring(vectorUsuario)).reversed());
        return sortedBuckets.subList(0, Math.min(cantidadBuckets, sortedBuckets.size()));
    }

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
