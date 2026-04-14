package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Feedback;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.NuevoFeedbackDto;
import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.FeedbackRepository;
import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {
  private final UsuariosRepository usuariosRepository;
  private final PublicacionesIntercambioRepository publicacionesIntercambioRepository;
  private final FeedbackRepository feedbackRepository;

  public FeedbackService(UsuariosRepository usuariosRepository, PublicacionesIntercambioRepository publicacionesIntercambioRepository,
                         FeedbackRepository feedbackRepository) {
    this.usuariosRepository = usuariosRepository;
    this.publicacionesIntercambioRepository = publicacionesIntercambioRepository;
    this.feedbackRepository = feedbackRepository;
  }

  public void publicarFeedback(NuevoFeedbackDto dto, Integer userId){
    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
    PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(dto.getPublicacionId()).orElseThrow(() -> new NotFoundException("Publicacion no encontrada"));

    if (!publicacion.getEstado().equals(EstadoPublicacion.FINALIZADA) && !publicacion.getPublicante().getId().equals(userId) || !publicacion.getPropuestaAceptada().getUsuario().getId().equals(userId)) {
      throw new UnauthorizedException("No tienes permisos para dejar un feedback en esta publicacion");
    }

    Optional<Feedback> feedbackExistente = feedbackRepository.findByPublicacionIntercambioIdAndCalificadorId(publicacion.getId(), userId);
    if (feedbackExistente.isPresent()) {
      throw new ConflictException("Ya has dejado un feedback en esta publicacion");
    }

    Feedback feedback = new Feedback();
    feedback.setComentario(dto.getComentario());
    feedback.setCalificacion(dto.getCalificacion());
    feedback.setPublicacionIntercambio(publicacion);
    feedback.setCalificador(usuario);
    feedbackRepository.save(feedback);
  }
}
