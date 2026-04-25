package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Feedback;
import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.PropuestaIntercambio;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.PropuestaMapper;
import com.tacs.tp1c2026.entities.dto.PublicacionMapper;
import com.tacs.tp1c2026.entities.dto.input.NewFeedbackDto;
import com.tacs.tp1c2026.entities.dto.input.PropuestaIntercambioDto;
import com.tacs.tp1c2026.entities.dto.input.PublicacionIntercambioDto;
import com.tacs.tp1c2026.entities.dto.input.ReviewProposalDto;
import com.tacs.tp1c2026.entities.dto.output.PaginacionDto;
import com.tacs.tp1c2026.entities.dto.output.PropuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.output.PublicacionDto;
import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.entities.enums.EstadoPropuesta;
import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import com.tacs.tp1c2026.entities.enums.ReviewAction;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
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
import org.springframework.beans.factory.annotation.Autowired;
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
  @Autowired
  private PublicacionesIntercambioRepository publicacionesIntercambioRepository;
  private final PropuestaMapper propuestaMapper;
  @Autowired
  private UsuariosService usuariosService;
  @Autowired
  private PublicacionesService publicacionesService;
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
  public void publicarIntercambioFigurita(PublicacionIntercambioDto dto, String userId) {
    Usuario usuario = usuariosService.obtenerUsuario(userId);
    //verificar que el usuario tiene la figurita en su coleccion
    Optional<FiguritaColeccion> itemUsuario = usuario.getCollection().stream().filter(
        f -> f.getFigurita().getId().equals(dto.getFiguritaId())).findFirst();
    if (itemUsuario.isEmpty()) {
      throw new ConflictException("No tiene la figurita en su coleccion");
    }

    FiguritaColeccion item = itemUsuario.get();
    if (!item.canPublishExchange(dto.getQuantity())) {
      throw new ConflictException("No tiene suficientes figuritas para publicar");
    }

    PublicacionIntercambio publicacion = PublicacionIntercambio.builder()
        .publicante(usuario)
        .figurita(item.getFigurita())
        .cantidad(dto.getQuantity())
        .build();
    publicacionesIntercambioRepository.save(publicacion);
    item.addCompromisedQuantity(dto.getQuantity(), TipoParticipacion.INTERCAMBIO);
    usuariosService.saveUser(usuario);
  }

  /**
   * Registra una propuesta de intercambio sobre una publicación existente.
   * Valida que haya cupos disponibles, que las figuritas ofrecidas pertenezcan al proponente
   * y que estén habilitadas para participar en intercambios.
   *
   * @return la {@link PropuestaIntercambio} persistida
   * @throws UserNotFoundException si el usuario no existe
   * @throws NotFoundException     si la publicación no existe
   * @throws ConflictException     si ya no hay cupos disponibles en la publicación
   * @throws BadInputException     si alguna de las figuritas no se encuentra o no está habilitada para intercambio
   */
  @Transactional
  public PropuestaIntercambio ofrecerPropuestaIntercambio(String userId, PropuestaIntercambioDto dto) {
    Usuario usuario = usuariosService.obtenerUsuario(userId);

    PublicacionIntercambio publicacion = getById(dto.getPublicacionId());

    // Validar cupos disponibles
    publicacion.validarCuposDisponibles();

    // Obtener y validar figuritas para oferta
    List<Figurita> figuritasParaOfrecer;

    figuritasParaOfrecer = usuario.obtenerFiguritasParaOferta(dto.getIdFiguritas());

    PropuestaIntercambio propuesta = new PropuestaIntercambio(publicacion, figuritasParaOfrecer, usuario);

    usuariosService.saveUser(usuario);
    propuesta = propuestasIntercambioRepository.save(propuesta);

    publicacion.agregarPropuesta(propuesta);
    publicacionesIntercambioRepository.save(publicacion);
    return propuesta;
  }

  /**
   * Retorna todas las propuestas de intercambio recibidas sobre las publicaciones propias del usuario.
   *
   * @param userId identificador del usuario publicante
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
   * Acepta una propuesta de intercambio pendiente y ejecuta la transferencia de figuritas.
   * Al aceptar: reduce el stock de la figurita publicada, agrega las figuritas ofrecidas
   * a la colección del publicante, elimina la figurita de sus faltantes si corresponde,
   * cierra la publicación cuando el stock llega a cero y rechaza automáticamente el resto
   * de propuestas pendientes.
   *
   * @param userId        identificador del usuario dueño de la publicación
   * @throws UserNotFoundException   si el usuario no existe
   * @throws NotFoundException       si la propuesta o la publicación no existen
   * @throws BadInputException       si la propuesta no corresponde a la publicación indicada
   * @throws UnauthorizedException   si el usuario no es el dueño de la publicación
   * @throws ConflictException       si la propuesta ya fue aceptada o rechazada previamente
   */
  @Transactional
  public void reviewProposal(ReviewProposalDto dto, String userId) {
    Usuario owner = usuariosService.obtenerUsuario(userId);
    PublicacionIntercambio publicacionIntercambio = getById(dto.getPublicacionId());
    publicacionIntercambio.validateOwner(owner);
    PropuestaIntercambio propuesta =  publicacionIntercambio.getPropuestas().stream().
        filter(propuestaIntercambio -> propuestaIntercambio.getId().equals(dto.getPropuestaId())).
        findFirst().orElseThrow(() -> new NotFoundException("No se encontro la propuesta"));
    if (!(propuesta.getEstado() == EstadoPropuesta.PENDIENTE)) {
      throw new ConflictException("La propuesta ya fue revisada");
    }
    Usuario proposer = propuesta.getUsuario();

    if (dto.getAction() == ReviewAction.REJECT){
      propuesta.rechazar();
      List<String> figuritasId = propuesta.getFiguritas().stream().map(Figurita::getId).toList();
      proposer.restoreFiguritasFromProposal(figuritasId, TipoParticipacion.INTERCAMBIO);
      propuestasIntercambioRepository.save(propuesta);
      usuariosService.saveUser(proposer);
    } else {
      propuesta.aceptar();
      publicacionIntercambio.aceptarPropuesta(propuesta, owner);
      //mover figus a colecciones
      owner.completeExchange(propuesta.getFiguritas(), publicacionIntercambio.getFigurita());
      //remove from proposer
      proposer.removeFromCollectionForExchange(propuesta.getFiguritas());
      if (publicacionIntercambio.getEstado() == EstadoPublicacion.FINALIZADA) {
        //rechazar las otras propuestas
        List<PropuestaIntercambio> proposalsToReject = publicacionIntercambio.getPropuestas().stream()
            .filter(p -> !p.getId().equals(propuesta.getId())).toList();

        proposalsToReject.forEach(proposal -> {
          proposal.rechazar();
          Usuario user = proposal.getUsuario();
          user.restoreFiguritasFromProposal(proposal.getFiguritas().stream().map(Figurita::getId).toList(), TipoParticipacion.INTERCAMBIO);
          usuariosService.saveUser(user);
        });
        propuestasIntercambioRepository.saveAll(proposalsToReject);
      }
      usuariosService.saveUser(proposer);
      usuariosService.saveUser(owner);
      publicacionesIntercambioRepository.save(publicacionIntercambio);
    }
  }

  public void publishFeedback(String userId, NewFeedbackDto dto) {
    Usuario usuario = usuariosService.obtenerUsuario(userId);
    PublicacionIntercambio publication = getById(dto.getPublicacionId());

    List<PropuestaIntercambio> acceptedProposals = publication.getPropuestasAceptada().stream().filter(propuesta -> propuesta.getUsuario().getId() == userId).toList();

    if (publication.getPublicante().getId() != userId) {
      if(acceptedProposals.isEmpty()) {
        throw new ConflictException("No eres dueño de la publicacion o tu propuesta no fue aceptada");
      }
    }

    List<Feedback> feedbacksPublicacion = publication.getFeedbacks();
    Optional<Feedback> existingFeedback = feedbacksPublicacion.stream().filter(
        feedback -> feedback.getCalificador().getId() == userId
    ).findFirst();
    if (existingFeedback.isPresent()) {
      throw new ConflictException("Ya dejaste un comentario sobre la publicacion");
    }
    Feedback feedback = new Feedback();
    feedback.setCalificacion(dto.getRating());
    feedback.setComentario(dto.getCommentary());
    feedback.setCalificador(usuario);
    publication.agregarFeedback(feedback);
    publicacionesIntercambioRepository.save(publication);
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

  public PublicacionIntercambio getById(String publicacionId) {
    return publicacionesIntercambioRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));
  }
}
