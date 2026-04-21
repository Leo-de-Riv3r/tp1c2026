/*package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicacionesIntercambioRepository extends JpaRepository<PublicacionIntercambio, Integer> {
  List<PublicacionIntercambio> findByPublicanteId(Integer userId);

  List<PublicacionIntercambio> findByEstado(EstadoPublicacion estadoPublicacion);

  Page<PublicacionIntercambio> buscarActivasConFiltros(
      @Param("estado") EstadoPublicacion estado,
      @Param("seleccion") String seleccion,
      @Param("nombreJugador") String nombreJugador,
      @Param("equipo") String equipo,
      @Param("categoria") Categoria categoria,
      Pageable pageable
  );
}
*/