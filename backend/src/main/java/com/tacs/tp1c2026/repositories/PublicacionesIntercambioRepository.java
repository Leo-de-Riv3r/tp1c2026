package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacionesIntercambioRepository extends JpaRepository<PublicacionIntercambio, Integer> {
  List<PublicacionIntercambio> findByPublicanteId(Integer userId);
}
