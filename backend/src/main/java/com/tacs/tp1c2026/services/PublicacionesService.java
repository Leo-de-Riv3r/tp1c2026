package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.PropuestaIntercambio;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.PropuestaMapper;
import com.tacs.tp1c2026.entities.dto.PublicacionMapper;
import com.tacs.tp1c2026.entities.dto.output.PaginacionDto;
import com.tacs.tp1c2026.entities.dto.output.PropuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.output.PublicacionDto;
import com.tacs.tp1c2026.entities.enums.Categoria;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PublicacionesService {
  private final PublicacionMapper publicacionMapper;
  private final PropuestasIntercambioRepository propuestasIntercambioRepository;
  private final UsuariosRepository usuariosRepository;
  private final PublicacionesIntercambioRepository publicacionesIntercambioRepository;
  private final PropuestaMapper propuestaMapper;

  public PublicacionesService(PublicacionMapper publicacionMapper, PropuestasIntercambioRepository propuestasIntercambioRepository, UsuariosRepository usuariosRepository, PublicacionesIntercambioRepository publicacionesIntercambioRepository, PropuestaMapper propuestaMapper) {
    this.publicacionMapper = publicacionMapper;
    this.propuestasIntercambioRepository = propuestasIntercambioRepository;
    this.usuariosRepository = usuariosRepository;
    this.publicacionesIntercambioRepository = publicacionesIntercambioRepository;
    this.propuestaMapper = propuestaMapper;
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

  public void ofrecerPropuestaIntercambio(Integer userId, Integer publicacionId, List<Integer> idFiguritas) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion con id " + publicacionId));
    //controlar que las ofertas no superen la cantidad disponibe
    List<PropuestaIntercambio> propuestasActuales = propuestasIntercambioRepository.findByPublicacionId(publicacionId);
    if (propuestasActuales.size() == publicacion.getFiguritaColeccion().getCantidad()) {
      throw new ConflictException("Ya no hay cupos para nuevas propuestas");
    }

    List<FiguritaColeccion> figuritas =
        usuario.getRepetidas().stream().filter(f -> idFiguritas.contains(f.getFigurita().getNumero())).toList();
    //filtrar figus que esten para intercambio
    figuritas = figuritas.stream().filter(figuritacoleccion -> figuritacoleccion.getTipoParticipacion().equals(TipoParticipacion.INTERCAMBIO)).toList();

    List<Figurita> figuritasOfrecidas = figuritas.stream().map(FiguritaColeccion::getFigurita).toList();
    if (figuritas.size() != idFiguritas.size()) {
      throw new BadInputException("No se encontraron todas las figuritas repetidas");
    }

    PropuestaIntercambio propuesta = new PropuestaIntercambio();

    propuesta.setPublicacion(publicacion);
    propuesta.setFiguritas(figuritasOfrecidas);
    propuesta.setUsuario(usuario);

    propuestasIntercambioRepository.save(propuesta);
  }

  public List<PropuestaRecibidaDto> obtenerPropuestasRecibidas(Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    List<PropuestaIntercambio> propuestasRecibidas = new ArrayList<>();
    List<PublicacionIntercambio> publicaciones = publicacionesIntercambioRepository.findByPublicanteId(userId);

    publicaciones.forEach(publicacion -> {
      List<PropuestaIntercambio> propuestas = propuestasIntercambioRepository.findByPublicacionId(publicacion.getId());
      propuestasRecibidas.addAll(propuestas);
    });

    return propuestaMapper.toDtoList(propuestasRecibidas);
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
    publicacion.setPropuestaAceptada(propuesta);
    publicacion.getFiguritaColeccion().reducirCantidad();
    //verificar figuritas de propuestas y agregar a coleccion
    propuesta.getFiguritas().forEach(figu -> {
      Optional<FiguritaColeccion> figuritaOptional = usuario.getRepetidas().stream().filter(
          f -> f.getFigurita().getNumero().equals(figu.getNumero())
      ).findFirst();

      if (figuritaOptional.isPresent()) {
        FiguritaColeccion figuritaColeccion = figuritaOptional.get();
        figuritaColeccion.aumentarCantidad();
      }

      if (usuario.getFaltantes().contains(figu)) {
        usuario.getFaltantes().remove(figu);
      }
    });

    if (publicacion.getFiguritaColeccion().getCantidad() == 0){
      publicacion.setEstado(EstadoPublicacion.FINALIZADA);
    }
    propuestasIntercambioRepository.save(propuesta);
    publicacionesIntercambioRepository.save(publicacion);
    //rechazo las otras propuestas
    //se deberia realizar luego de responder al user
    List<PropuestaIntercambio> propuestas = propuestasIntercambioRepository.findByPublicacionId(publicacionId);
    propuestas.stream().filter(p -> !p.getId().equals(propuestaId)).forEach(p -> p.rechazar());
    //figurita coleccion es de usuario
    propuestasIntercambioRepository.saveAll(propuestas);
  }

  public PaginacionDto<PublicacionDto> buscarPublicaciones(
      String seleccion, String nombreJugador, String equipo, Categoria categoria, Integer page, Integer per_page
  ) {
    //primero obtener publicaciones activas
    List<PublicacionIntercambio> publicaciones = publicacionesIntercambioRepository.findByEstado(EstadoPublicacion.ACTIVA);

    //podria dejar el filtrado en otra seccion del codigo para no cargar mucho el service

    if(!publicaciones.isEmpty()) {
      //luego reemplazar por filtros de mongodb

      if(seleccion != null) {
        publicaciones = publicaciones.stream().filter(publicacion ->
        publicacion.getFiguritaColeccion().getFigurita().getSeleccion().contains(seleccion)
        ).toList();
      }

      if(nombreJugador != null) {
        publicaciones = publicaciones.stream().filter(publicacion ->
            publicacion.getFiguritaColeccion().getFigurita().getJugador().contains(seleccion)
        ).toList();
      }

      if(equipo != null) {
        publicaciones = publicaciones.stream().filter(publicacion ->
            publicacion.getFiguritaColeccion().getFigurita().getEquipo().contains(equipo)
        ).toList();
      }

      if (categoria != null) {
        publicaciones = publicaciones.stream().filter(publicacion ->
            publicacion.getFiguritaColeccion().getFigurita().getCategoria().equals(categoria)
        ).toList();
      }
      //verificar num paginas y armar resultado paginado
      int totalPages = (int) Math.ceil((double) publicaciones.size() / per_page);
      int startIndex = (page - 1) * per_page;
      int endIndex = Math.min(startIndex + per_page, publicaciones.size());
      List<PublicacionIntercambio> paginatedPublicaciones = publicaciones.subList(startIndex, endIndex);
      //map publicaciones to dtos
      List<PublicacionDto> mapeados = publicacionMapper.mapToDto(paginatedPublicaciones);

      return new PaginacionDto<>(mapeados, page, totalPages);
    } else {
      return new PaginacionDto<>(null, 1, 1);
    }

  }
}
