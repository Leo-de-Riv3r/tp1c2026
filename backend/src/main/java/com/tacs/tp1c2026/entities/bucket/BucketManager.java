package com.tacs.tp1c2026.entities.bucket;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.tacs.tp1c2026.entities.Usuario;

public class BucketManager {

    private static final String BUCKET_COUNT_PROPERTY = "app.bucket.count";
    private static final int DEFAULT_BUCKET_COUNT = 10;
    public static final int CANTIDAD_FIGURITAS = 5000;
    private static final int MAX_BUCKETS_PER_USER = 3;
    private static final int MAX_BUCKETS_PER_QUERY = 5;

    private final List<Bucket> buckets;

    private BucketManager() {
        int bucketCount = loadBucketCountFromProperties();
        List<Bucket> bucketList = new ArrayList<>(bucketCount);
        for (int i = 0; i < bucketCount; i++) {
            bucketList.add(new Bucket());
        }
        this.buckets = Collections.synchronizedList(bucketList);
    }

    private static int loadBucketCountFromProperties() {
        Properties properties = new Properties();
        try (InputStream stream = BucketManager.class.getResourceAsStream("/application.properties")) {
            if (stream != null) {
                properties.load(stream);
                String value = properties.getProperty(BUCKET_COUNT_PROPERTY);
                if (value != null && !value.isBlank()) {
                    return Integer.parseInt(value.trim());
                }
            }
        } catch (IOException | NumberFormatException ignored) {
        }
        return DEFAULT_BUCKET_COUNT;
    }

    private static class Holder {
        private static final BucketManager INSTANCE = new BucketManager();
    }

    public static BucketManager getInstance() {
        return Holder.INSTANCE;
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    public void updateBuckets(Usuario usuario, short[] vectorUsuario) {

        this.buckets.forEach(b -> b.removerVecino(usuario));

        List<Bucket> sortedBuckets = new ArrayList<>(this.buckets);
        sortedBuckets.sort((b1, b2) -> Integer.compare(b2.calcularScoring(vectorUsuario), b1.calcularScoring(vectorUsuario)));

        for (int i = 0; i < Math.min(MAX_BUCKETS_PER_USER, sortedBuckets.size()); i++) {
            sortedBuckets.get(i).agregarVecino(usuario);
        }

    }

    public List<Bucket> queryBuckets(short[] vectorUsuario) {
        List<Bucket> sortedBuckets = new ArrayList<>(this.buckets);
        sortedBuckets.sort((b1, b2) -> Integer.compare(b2.calcularScoring(vectorUsuario), b1.calcularScoring(vectorUsuario)));
        return sortedBuckets.subList(0, Math.min(MAX_BUCKETS_PER_QUERY, sortedBuckets.size()));
    }

    public Bucket findNearestBucket(short[] vectorUsuario) {
        return this.buckets.stream()
            .max((b1, b2) -> Integer.compare(b1.calcularScoring(vectorUsuario), b2.calcularScoring(vectorUsuario)))
            .orElse(null);
    }

}
