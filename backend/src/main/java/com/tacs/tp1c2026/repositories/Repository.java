package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.exceptions.NotFoundException;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repositorio genérico que agrega un método de conveniencia para lanzar una NotFoundException cuando una entidad
 * con el id dado no está presente.
 */
@NoRepositoryBean
public interface Repository<T, ID> extends MongoRepository<T, ID> {

  default T findOrThrow(ID id) {
    return this.findById(id).orElseThrow(() -> new NotFoundException("Recurso no encontrado"));
  }
}

