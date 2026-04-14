package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacionesIntercambioRepository extends JpaRepository<PublicacionIntercambio, Integer> {
  List<PublicacionIntercambio> findByEstado(EstadoPublicacion estadoPublicacion);
  // Spring entiende que debe entrar a 'publicante' y buscar por su 'id'
  List<PublicacionIntercambio> findByPublicanteId(Integer usuarioId);
}
