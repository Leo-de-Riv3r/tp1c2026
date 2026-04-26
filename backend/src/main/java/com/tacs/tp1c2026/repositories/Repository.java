package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.exceptions.NotFoundException;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Generic repository that adds a convenience method to throw a NotFoundException when an entity
 * with the given id is not present.
 */
public interface Repository<T, ID> extends MongoRepository<T, ID> {

  default T findOrThrow(ID id) {
    return this.findById(id).orElseThrow(() -> new NotFoundException("Resource not found"));
  }
}

