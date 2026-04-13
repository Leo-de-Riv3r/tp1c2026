package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.bucket.Bucket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BucketRepository extends JpaRepository<Bucket, Integer> {
}
