package com.tacs.tp1c2026.scheduled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.bucket.Bucket;
import com.tacs.tp1c2026.entities.bucket.BucketManager;
import com.tacs.tp1c2026.repositories.FiguritasRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;


@Component
public class GeneradorSugerencias {

    private final UsuariosRepository usuariosRepository;

    public GeneradorSugerencias(UsuariosRepository usuariosRepository, FiguritasRepository figuritasRepository) {
        this.usuariosRepository = usuariosRepository;
    }

    @Scheduled(cron = "${app.scheduled.example.cron:0 0 * * * *}")
    public void generarSugerencias() {

        List<Usuario> usuarios = usuariosRepository.findAll();
        Map<Bucket, List<Usuario>> nearestNeighbors = new HashMap<>();

        for (Bucket bucket : BucketManager.getInstance().getBuckets()) {
            nearestNeighbors.put(bucket, List.of());
        }
        

        for (Usuario usuario : usuarios) {

            usuario.getSugerenciasIntercambios().clear();
            List<Bucket> buckets = BucketManager.getInstance().queryBuckets(usuario.getQueryVector());

            for (Bucket bucket : buckets) {
                bucket.getVecinos().stream()
                    .filter(vecino -> !vecino.getId().equals(usuario.getId()))
                    .forEach(vecino -> {
                        usuario.getSugerenciasIntercambios().add(vecino);
                    });
            }

            Bucket bucket = BucketManager.getInstance().findNearestBucket(usuario.getQueryVector());
            
            nearestNeighbors.get(bucket).add(usuario);

        }

        for (Bucket bucket : nearestNeighbors.keySet()) {
            List<Usuario> vecinos = nearestNeighbors.get(bucket);
            bucket.updateVector(vecinos.stream().map(Usuario::getQueryVector).toList());
        }

        



    }
}
