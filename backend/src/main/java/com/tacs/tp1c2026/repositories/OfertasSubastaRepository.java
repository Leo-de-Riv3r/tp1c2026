package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.OfertaSubasta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface OfertasSubastaRepository extends JpaRepository<OfertaSubasta, Integer>{
  List<OfertaSubasta> findBySubastaId(Integer subastaId);
  List<OfertaSubasta> findByUsuarioPostorId(Integer userId);
  List<OfertaSubasta> findBySubastaUsuarioPublicanteId(Integer userId);
}
