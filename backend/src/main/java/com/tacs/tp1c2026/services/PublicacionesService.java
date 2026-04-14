package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.PropuestaIntercambio;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.enums.EstadoPropuesta;
import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.PropuestasIntercambioRepository;
import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PublicacionesService {
  private final PropuestasIntercambioRepository propuestasIntercambioRepository;
  private final UsuariosRepository usuariosRepository;
  private final PublicacionesIntercambioRepository publicacionesIntercambioRepository;

  public PublicacionesService(PropuestasIntercambioRepository propuestasIntercambioRepository, UsuariosRepository usuariosRepository, PublicacionesIntercambioRepository publicacionesIntercambioRepository) {
    this.propuestasIntercambioRepository = propuestasIntercambioRepository;
    this.usuariosRepository = usuariosRepository;
    this.publicacionesIntercambioRepository = publicacionesIntercambioRepository;
  }

  public void publicarIntercambioFigurita(Integer userId, Integer numFigurita){
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    Optional<FiguritaColeccion> figuritaOptional = usuario.getRepetidas().stream().filter(f -> f.getFigurita().getNumero().equals(numFigurita)).findFirst();
    FiguritaColeccion figurita = figuritaOptional.orElseThrow(() -> new NotFoundException("No se encontro la figurita repetida"));

    PublicacionIntercambio publicacion = new PublicacionIntercambio();
    publicacion.setFiguritaColeccion(figurita);
    publicacion.setPublicante(usuario);

    publicacionesIntercambioRepository.save(publicacion);
  }

  public PropuestaIntercambio ofrecerPropuestaIntercambio(Integer userId, Integer publicacionId, List<Integer> idFiguritas) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    List<FiguritaColeccion> figuritas =
        usuario.getRepetidas().stream().filter(f -> idFiguritas.contains(f.getFigurita().getNumero())).toList();
    //filtrar figus que esten para intercambio
    figuritas = figuritas.stream().filter(figuritacoleccion -> figuritacoleccion.getTipoParticipacion().equals(TipoParticipacion.INTERCAMBIO)).toList();

    List<Figurita> figuritasOfrecidas = figuritas.stream().map(FiguritaColeccion::getFigurita).toList();
    if (figuritas.size() != idFiguritas.size()) {
      throw new BadInputException("No se encontraron todas las figuritas repetidas");
    }

    PropuestaIntercambio propuesta = new PropuestaIntercambio();
    PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));

    propuesta.setPublicacion(publicacion);
    propuesta.setFiguritas(figuritasOfrecidas);
    propuesta.setUsuario(usuario);

    propuestasIntercambioRepository.save(propuesta);
    return propuesta;
  }

  public void rechazarPropuesta(Integer publicacionId, Integer propuestaId, Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    PropuestaIntercambio propuesta = propuestasIntercambioRepository.findById(propuestaId)
        .orElseThrow(() -> new NotFoundException("No se encontro la propuesta"));

    PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));

    if (!propuesta.getPublicacion().getId().equals(publicacion.getId())) {
      throw new BadInputException("La publicacion no corresponde a la propuesta");
    }

    if (!propuesta.getUsuario().equals(usuario)) {
      throw new UnauthorizedException("El usuario no es el dueño de la propuesta");
    }

    if(!propuesta.getEstado().equals(EstadoPropuesta.PENDIENTE)) {
      throw new ConflictException("La propuesta ya fue aceptada o rechazada");
    }

    propuesta.rechazar();
    propuestasIntercambioRepository.save(propuesta);
  }

  public void aceptarPropuesta(Integer publicacionId, Integer propuestaId, Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    PropuestaIntercambio propuesta = propuestasIntercambioRepository.findById(propuestaId)
        .orElseThrow(() -> new NotFoundException("No se encontro la propuesta"));

    PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));

    if (!propuesta.getPublicacion().getId().equals(publicacion.getId())) {
      throw new BadInputException("La publicacion no corresponde a la propuesta");
    }

    if (!publicacion.getPublicante().equals(usuario)) {
      throw new UnauthorizedException("El usuario no es el dueño de la publicacion");
    }

    if(!propuesta.getEstado().equals(EstadoPropuesta.PENDIENTE)) {
      throw new ConflictException("La propuesta ya fue aceptada o rechazada");
    }

    propuesta.aceptar();
    publicacion.setEstado(EstadoPublicacion.FINALIZADA);
    publicacion.setPropuestaAceptada(propuesta);
    propuestasIntercambioRepository.save(propuesta);
    publicacionesIntercambioRepository.save(publicacion);
    List<PropuestaIntercambio> propuestas = propuestasIntercambioRepository.findByPublicacionId(publicacionId);
    propuestas.stream().filter(p -> !p.getId().equals(propuestaId)).forEach(p -> p.rechazar());
    propuestasIntercambioRepository.saveAll(propuestas);
  }
}
