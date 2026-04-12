package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Figurita;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiguritasRepository extends JpaRepository<Figurita, Integer> {
  Optional<Figurita> findByNumero(Integer numFigurita);
}
