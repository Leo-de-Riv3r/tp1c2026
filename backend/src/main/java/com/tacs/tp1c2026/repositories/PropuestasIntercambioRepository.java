package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.PropuestaIntercambio;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PropuestasIntercambioRepository extends MongoRepository<PropuestaIntercambio, String> {
  List<PropuestaIntercambio> findByPublicacionId(Integer publicacionId);
  List<PropuestaIntercambio> findByUsuarioId(Integer userId);
  List<PropuestaIntercambio> findByPublicacionPublicanteId(Integer userId); // dueño de la publicacion que recibe propuestas
}
