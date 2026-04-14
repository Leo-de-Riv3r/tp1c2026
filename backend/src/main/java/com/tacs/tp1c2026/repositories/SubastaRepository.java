package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Subasta;
import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubastaRepository extends JpaRepository<Subasta, Integer> {
   List<Subasta> findByUsuarioPublicanteId(Integer userId);
   List<Subasta> findByUsuarioPublicanteIdAndEstado(Integer userId, EstadoSubasta estado);
   List<Subasta> findByEstado(EstadoSubasta estado);
}
