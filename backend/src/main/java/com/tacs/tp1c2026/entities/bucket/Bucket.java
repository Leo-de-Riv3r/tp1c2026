package com.tacs.tp1c2026.entities.bucket;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tacs.tp1c2026.entities.Usuario;

import jakarta.persistence.Entity;

public class Bucket {

    private short[] vectorRepresentativo = new short[BucketManager.CANTIDAD_FIGURITAS];

    private Set<Usuario> vecinos = new HashSet<>();

    public int calcularScoring(short[] vectorUsuario) {
        int score = 0;
        for (int i = 0; i < BucketManager.CANTIDAD_FIGURITAS; i++) {
            if ((vectorUsuario[i] == 1 && vectorRepresentativo[i] == 1) || (vectorUsuario[i] == -1 && vectorRepresentativo[i] == -1)) {
                score++;
            }
        }
        return score;
    }

    public void agregarVecino(Usuario usuario) {
        this.vecinos.add(usuario);
    }

    public void removerVecino(Usuario usuario) {
        this.vecinos.remove(usuario);
    }

    public Set<Usuario> getVecinos() {
        return vecinos;
    }

    public void updateVector(List<short[]> vectors) {

        if (vectors.isEmpty()) {;
            return;
        }

        // compute average vector
        short[] newVector = new short[BucketManager.CANTIDAD_FIGURITAS];
        for (short[] vector : vectors) {
            for (int i = 0; i < BucketManager.CANTIDAD_FIGURITAS; i++) {
                newVector[i] += vector[i];
            }
        }
        for (int i = 0; i < BucketManager.CANTIDAD_FIGURITAS; i++) {
            newVector[i] = (short) (newVector[i] / vectors.size());
        }

        // round to nearest {-1, 0, 1}
        for (int i = 0; i < BucketManager.CANTIDAD_FIGURITAS; i++) {
            if (newVector[i] > 0) {
                newVector[i] = 1;
            } else if (newVector[i] < 0) {
                newVector[i] = -1;
            } else {
                newVector[i] = 0;
            }
        }

        this.vectorRepresentativo = newVector;
    }

}
