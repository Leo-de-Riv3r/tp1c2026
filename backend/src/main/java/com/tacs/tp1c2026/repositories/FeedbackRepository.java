/*package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Feedback;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

  Optional<Feedback> findByPublicacionIntercambioIdAndPublicacionIntercambioPublicanteId(
      Integer publicacionId,
      Integer usuarioId
  );
  //find by publicacion id and usuario id
  Optional<Feedback> findByPublicacionIntercambioIdAndCalificadorId(Integer publicacionId, Integer calificadorId);
}*/