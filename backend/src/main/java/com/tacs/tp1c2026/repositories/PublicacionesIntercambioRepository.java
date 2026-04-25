package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

public interface PublicacionesIntercambioRepository extends MongoRepository<PublicacionIntercambio, String> {
  List<PublicacionIntercambio> findByPublicanteId(Integer userId);

//  Page<PublicacionIntercambio> buscarActivasConFiltros(
//      @Param("estado") EstadoPublicacion estado,
//      @Param("seleccion") String seleccion,
//      @Param("nombreJugador") String nombreJugador,
//      @Param("equipo") String equipo,
//      @Param("categoria") Categoria categoria,
//      Pageable pageable
//  );
}
