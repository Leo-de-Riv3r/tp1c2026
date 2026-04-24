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
import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.CuposAgotadosException;
import com.tacs.tp1c2026.exceptions.FiguritaNoDisponibleException;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.FiguritaYaPublicadaException;
import com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.PropuestaNoCorrespondeException;
import com.tacs.tp1c2026.exceptions.PropuestaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.exceptions.UsuarioNoAutorizadoException;
import com.tacs.tp1c2026.repositories.PropuestasIntercambioRepository;
import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  /**
   * Crea una publicación de intercambio para una figurita repetida del usuario.
   * Verifica que el usuario exista y que la figurita indicada esté en su colección de repetidas.
   *
   * @param userId     identificador del usuario que realiza la publicación
   * @param numFigurita número de la figurita repetida a publicar
   * @throws UserNotFoundException si el usuario no existe
   * @throws NotFoundException     si la figurita no se encuentra entre las repetidas del usuario
   */
  @Transactional
  public void publicarIntercambioFigurita(Integer userId, Integer numFigurita) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    try {
      PublicacionIntercambio publicacion = usuario.publicarFigurita(numFigurita);
      publicacionesIntercambioRepository.save(publicacion);
    } catch (FiguritaNoEncontradaException e) {
      throw new NotFoundException(e.getMessage());
    } catch (FiguritaYaPublicadaException | FiguritasInsuficientesException e) {
      throw new ConflictException(e.getMessage());
    }
  }

  /**
   * Registra una propuesta de intercambio sobre una publicación existente.
   * Valida que haya cupos disponibles, que las figuritas ofrecidas pertenezcan al proponente
   * y que estén habilitadas para participar en intercambios.
   *
   * @param userId        identificador del usuario que realiza la propuesta
   * @param publicacionId identificador de la publicación sobre la que se propone el intercambio
   * @param idFiguritas   lista de números de figurita que se ofrecen como contraprestación
   * @return la {@link PropuestaIntercambio} persistida
   * @throws UserNotFoundException si el usuario no existe
   * @throws NotFoundException     si la publicación no existe
   * @throws ConflictException     si ya no hay cupos disponibles en la publicación
   * @throws BadInputException     si alguna de las figuritas no se encuentra o no está habilitada para intercambio
   */
  @Transactional
  public PropuestaIntercambio ofrecerPropuestaIntercambio(Integer userId, Integer publicacionId, List<Integer> idFiguritas) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion con id " + publicacionId));

    // Validar cupos disponibles
    try {
      publicacion.validarCuposDisponibles();
    } catch (CuposAgotadosException e) {
      throw new ConflictException(e.getMessage());
    }

    // Obtener y validar figuritas para oferta
    List<FiguritaColeccion> figuritasParaOfrecer;
    try {
      figuritasParaOfrecer = usuario.obtenerFiguritasParaOferta(idFiguritas);
    } catch (FiguritaNoEncontradaException | FiguritaNoDisponibleException e) {
      throw new BadInputException(e.getMessage());
    }


    List<Figurita> figuritasOfrecidas = figuritasParaOfrecer.stream()
        .map(FiguritaColeccion::getFigurita)
        .toList();

    PropuestaIntercambio propuesta = new PropuestaIntercambio(publicacion, figuritasOfrecidas, usuario);

    publicacion.agregarPropuesta(propuesta);
    propuestasIntercambioRepository.save(propuesta);
    return propuesta;
  }

  /**
   * Retorna todas las propuestas de intercambio recibidas sobre las publicaciones propias del usuario.
   *
   * @param userId identificador del usuario publicante
   * @return lista de {@link PropuestaRecibidaDto} con los datos de cada propuesta recibida
   * @throws UserNotFoundException si el usuario no existe
   */
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

  /**
   * Rechaza una propuesta de intercambio pendiente.
   * Verifica que la propuesta y la publicación estén relacionadas, que el usuario sea el dueño
   * de la publicación y que la propuesta aún se encuentre en estado PENDIENTE.
   *
   * @param publicacionId identificador de la publicación de intercambio
   * @param propuestaId   identificador de la propuesta a rechazar
   * @param userId        identificador del usuario dueño de la publicación
   * @throws UserNotFoundException   si el usuario no existe
   * @throws NotFoundException       si la propuesta o la publicación no existen
   * @throws BadInputException       si la propuesta no corresponde a la publicación indicada
   * @throws UnauthorizedException   si el usuario no es el dueño de la publicación
   * @throws ConflictException       si la propuesta ya fue aceptada o rechazada previamente
   */
  @Transactional
  public void rechazarPropuesta(Integer publicacionId, Integer propuestaId, Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    PropuestaIntercambio propuesta = propuestasIntercambioRepository.findById(propuestaId)
        .orElseThrow(() -> new NotFoundException("No se encontro la propuesta"));

    PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));

    try {
      publicacion.rechazarPropuesta(propuesta, usuario);
    } catch (PropuestaNoCorrespondeException e) {
      throw new BadInputException(e.getMessage());
    } catch (UsuarioNoAutorizadoException e) {
      throw new UnauthorizedException(e.getMessage());
    } catch (PropuestaYaProcesadaException e) {
      throw new ConflictException(e.getMessage());
    }

    propuestasIntercambioRepository.save(propuesta);
  }

  /**
   * Acepta una propuesta de intercambio pendiente y ejecuta la transferencia de figuritas.
   * Al aceptar: reduce el stock de la figurita publicada, agrega las figuritas ofrecidas
   * a la colección del publicante, elimina la figurita de sus faltantes si corresponde,
   * cierra la publicación cuando el stock llega a cero y rechaza automáticamente el resto
   * de propuestas pendientes.
   *
   * @param publicacionId identificador de la publicación de intercambio
   * @param propuestaId   identificador de la propuesta a aceptar
   * @param userId        identificador del usuario dueño de la publicación
   * @throws UserNotFoundException   si el usuario no existe
   * @throws NotFoundException       si la propuesta o la publicación no existen
   * @throws BadInputException       si la propuesta no corresponde a la publicación indicada
   * @throws UnauthorizedException   si el usuario no es el dueño de la publicación
   * @throws ConflictException       si la propuesta ya fue aceptada o rechazada previamente
   */
  @Transactional
  public void aceptarPropuesta(Integer publicacionId, Integer propuestaId, Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    PropuestaIntercambio propuesta = propuestasIntercambioRepository.findById(propuestaId)
        .orElseThrow(() -> new NotFoundException("No se encontro la propuesta"));

    PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));

    try {
      publicacion.aceptarPropuesta(propuesta, usuario);
    } catch (PropuestaNoCorrespondeException e) {
      throw new BadInputException(e.getMessage());
    } catch (UsuarioNoAutorizadoException e) {
      throw new UnauthorizedException(e.getMessage());
    } catch (PropuestaYaProcesadaException e) {
      throw new ConflictException(e.getMessage());
    }

    propuestasIntercambioRepository.save(propuesta);
    publicacionesIntercambioRepository.save(publicacion);
  }

  /**
   * Busca publicaciones de intercambio activas aplicando filtros opcionales y devuelve el resultado paginado.
   *
   * @param seleccion     filtra por nombre de selección (subcadena, puede ser {@code null})
   * @param nombreJugador filtra por nombre de jugador (subcadena, puede ser {@code null})
   * @param equipo        filtra por nombre de equipo (subcadena, puede ser {@code null})
   * @param categoria     filtra por categoría de la figurita (puede ser {@code null})
   * @param page          número de página solicitada (1-based)
   * @param per_page      cantidad de resultados por página
   * @return {@link PaginacionDto} con la lista de {@link PublicacionDto} correspondiente a la página solicitada
   */
  public PaginacionDto<PublicacionDto> buscarPublicaciones(
      String seleccion, String nombreJugador, String equipo, Categoria categoria, Integer page, Integer per_page
  ) {
    int paginaSolicitada = (page == null || page < 1) ? 1 : page;
    int tamanioPagina = (per_page == null || per_page < 1) ? 10 : per_page;
    Pageable pageable = PageRequest.of(paginaSolicitada - 1, tamanioPagina);

    Page<PublicacionIntercambio> publicaciones = publicacionesIntercambioRepository.buscarActivasConFiltros(
        EstadoPublicacion.ACTIVA,
        normalizeFilter(seleccion),
        normalizeFilter(nombreJugador),
        normalizeFilter(equipo),
        categoria,
        pageable
    );

    if (publicaciones.getTotalElements() == 0) {
      return new PaginacionDto<>(null, 1, 1);
    }

    List<PublicacionDto> mapeados = publicacionMapper.mapToDto(publicaciones.getContent());
    int totalPages = Math.max(1, publicaciones.getTotalPages());
    return new PaginacionDto<>(mapeados, paginaSolicitada, totalPages);

  }

  private String normalizeFilter(String filterValue) {
    if (filterValue == null || filterValue.isBlank()) {
      return null;
    }
    return filterValue.trim();

  }
}
