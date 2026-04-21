// package com.tacs.tp1c2026.services;

// import com.tacs.tp1c2026.entities.Figurita;
// import com.tacs.tp1c2026.entities.FiguritaColeccion;
// import com.tacs.tp1c2026.entities.PropuestaIntercambio;
// import com.tacs.tp1c2026.entities.PublicacionIntercambio;
// import com.tacs.tp1c2026.entities.Usuario;
// import com.tacs.tp1c2026.entities.dto.PropuestaMapper;
// import com.tacs.tp1c2026.entities.dto.PublicacionMapper;
// import com.tacs.tp1c2026.entities.dto.output.PaginacionDto;
// import com.tacs.tp1c2026.entities.dto.output.PropuestaRecibidaDto;
// import com.tacs.tp1c2026.entities.dto.output.PublicacionDto;
// import com.tacs.tp1c2026.entities.enums.Categoria;
// import com.tacs.tp1c2026.entities.enums.EstadoPropuesta;
// import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
// import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
// import com.tacs.tp1c2026.exceptions.BadInputException;
// import com.tacs.tp1c2026.exceptions.ConflictException;
// import com.tacs.tp1c2026.exceptions.NotFoundException;
// import com.tacs.tp1c2026.exceptions.UnauthorizedException;
// import com.tacs.tp1c2026.exceptions.UserNotFoundException;
// import com.tacs.tp1c2026.repositories.PropuestasIntercambioRepository;
// import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
// import com.tacs.tp1c2026.repositories.UsuariosRepository;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// @Slf4j
// @Service
// public class PublicacionesService {
//   private final PublicacionMapper publicacionMapper;
//   private final PropuestasIntercambioRepository propuestasIntercambioRepository;
//   private final UsuariosRepository usuariosRepository;
//   private final PublicacionesIntercambioRepository publicacionesIntercambioRepository;
//   private final PropuestaMapper propuestaMapper;

//   public PublicacionesService(PublicacionMapper publicacionMapper, PropuestasIntercambioRepository propuestasIntercambioRepository, UsuariosRepository usuariosRepository, PublicacionesIntercambioRepository publicacionesIntercambioRepository, PropuestaMapper propuestaMapper) {
//     this.publicacionMapper = publicacionMapper;
//     this.propuestasIntercambioRepository = propuestasIntercambioRepository;
//     this.usuariosRepository = usuariosRepository;
//     this.publicacionesIntercambioRepository = publicacionesIntercambioRepository;
//     this.propuestaMapper = propuestaMapper;
//   }

//   /**
//    * Crea una publicación de intercambio para una figurita repetida del usuario.
//    * Verifica que el usuario exista y que la figurita indicada esté en su colección de repetidas.
//    *
//    * @param userId     identificador del usuario que realiza la publicación
//    * @param numFigurita número de la figurita repetida a publicar
//    * @throws UserNotFoundException si el usuario no existe
//    * @throws NotFoundException     si la figurita no se encuentra entre las repetidas del usuario
//    */
//   @Transactional
//   public void publicarIntercambioFigurita(Integer userId, Integer numFigurita){
//     Usuario usuario = usuariosRepository.findById(userId)
//         .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

//     Optional<FiguritaColeccion> figuritaOptional = usuario.getRepetidas().stream().filter(f -> f.getFigurita().getNumero().equals(numFigurita)).findFirst();
//     FiguritaColeccion figurita = figuritaOptional.orElseThrow(() -> new NotFoundException("No se encontro la figurita repetida"));

//     if(figurita.getPublicada()) {
//       throw new ConflictException("La figurita ya se encuentra publicada");
//     }

//     if(figurita.getCantidad() == 0) {
//       throw new ConflictException("Ya no tienes la figurita repetida");
//     }

//     figurita.setPublicada(true);
//     PublicacionIntercambio publicacion = new PublicacionIntercambio();
//     publicacion.setFiguritaColeccion(figurita);
//     publicacion.setPublicante(usuario);

//     publicacionesIntercambioRepository.save(publicacion);
//   }

//   /**
//    * Registra una propuesta de intercambio sobre una publicación existente.
//    * Valida que haya cupos disponibles, que las figuritas ofrecidas pertenezcan al proponente
//    * y que estén habilitadas para participar en intercambios.
//    *
//    * @param userId        identificador del usuario que realiza la propuesta
//    * @param publicacionId identificador de la publicación sobre la que se propone el intercambio
//    * @param idFiguritas   lista de números de figurita que se ofrecen como contraprestación
//    * @return la {@link PropuestaIntercambio} persistida
//    * @throws UserNotFoundException si el usuario no existe
//    * @throws NotFoundException     si la publicación no existe
//    * @throws ConflictException     si ya no hay cupos disponibles en la publicación
//    * @throws BadInputException     si alguna de las figuritas no se encuentra o no está habilitada para intercambio
//    */
//   @Transactional
//   public PropuestaIntercambio ofrecerPropuestaIntercambio(Integer userId, Integer publicacionId, List<Integer> idFiguritas) {
//     Usuario usuario = usuariosRepository.findById(userId)
//         .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

//     PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
//         .orElseThrow(() -> new NotFoundException("No se encontro la publicacion con id " + publicacionId));
//     //controlar que las ofertas no superen la cantidad disponibe
//     List<PropuestaIntercambio> propuestasActuales = propuestasIntercambioRepository.findByPublicacionId(publicacionId);
//     if (propuestasActuales.size() == publicacion.getFiguritaColeccion().getCantidad()) {
//       throw new ConflictException("Ya no hay cupos para nuevas propuestas");
//     }

//     List<FiguritaColeccion> figuritas =
//         usuario.getRepetidas().stream().filter(f -> idFiguritas.contains(f.getFigurita().getNumero())).toList();
//     //filtrar figus que esten para intercambio
//     figuritas = figuritas.stream().filter(figuritacoleccion ->
//         figuritacoleccion.getTipoParticipacion().equals(TipoParticipacion.INTERCAMBIO) &&
//             !figuritacoleccion.getPublicada()  &&
//             figuritacoleccion.getCantidadOfertada() < figuritacoleccion.getCantidad()
//      ).toList();

//     List<Figurita> figuritasOfrecidas = figuritas.stream().map(FiguritaColeccion::getFigurita).toList();
//     if (figuritas.size() != idFiguritas.size()) {
//       throw new BadInputException("No se encontraron todas las figuritas repetidas u algunas no se pueden ofertar");
//     }

//     //actualizar figuritas de usuario
//     figuritas.forEach(figurita ->
//         figurita.aumentarCantidadOfertada()
//     );


//     PropuestaIntercambio propuesta = new PropuestaIntercambio();

//     propuesta.setPublicacion(publicacion);
//     propuesta.setFiguritas(figuritasOfrecidas);
//     propuesta.setUsuario(usuario);

//     propuestasIntercambioRepository.save(propuesta);
//     return propuesta;
//   }

//   /**
//    * Retorna todas las propuestas de intercambio recibidas sobre las publicaciones propias del usuario.
//    *
//    * @param userId identificador del usuario publicante
//    * @return lista de {@link PropuestaRecibidaDto} con los datos de cada propuesta recibida
//    * @throws UserNotFoundException si el usuario no existe
//    */
//   public List<PropuestaRecibidaDto> obtenerPropuestasRecibidas(Integer userId) {
//     Usuario usuario = usuariosRepository.findById(userId)
//         .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

//     List<PropuestaIntercambio> propuestasRecibidas = new ArrayList<>();
//     List<PublicacionIntercambio> publicaciones = publicacionesIntercambioRepository.findByPublicanteId(userId);

//     publicaciones.forEach(publicacion -> {
//       List<PropuestaIntercambio> propuestas = propuestasIntercambioRepository.findByPublicacionId(publicacion.getId());
//       propuestasRecibidas.addAll(propuestas);
//     });

//     return propuestaMapper.toDtoList(propuestasRecibidas);
//   }
//   /**
//    * Rechaza una propuesta de intercambio pendiente.
//    * Verifica que la propuesta y la publicación estén relacionadas, que el usuario sea el dueño
//    * de la publicación y que la propuesta aún se encuentre en estado PENDIENTE.
//    *
//    * @param publicacionId identificador de la publicación de intercambio
//    * @param propuestaId   identificador de la propuesta a rechazar
//    * @param userId        identificador del usuario dueño de la publicación
//    * @throws UserNotFoundException   si el usuario no existe
//    * @throws NotFoundException       si la propuesta o la publicación no existen
//    * @throws BadInputException       si la propuesta no corresponde a la publicación indicada
//    * @throws UnauthorizedException   si el usuario no es el dueño de la publicación
//    * @throws ConflictException       si la propuesta ya fue aceptada o rechazada previamente
//    */
//   @Transactional
//   public void rechazarPropuesta(Integer publicacionId, Integer propuestaId, Integer userId) {
//     Usuario usuario = usuariosRepository.findById(userId)
//         .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

//     PropuestaIntercambio propuesta = propuestasIntercambioRepository.findById(propuestaId)
//         .orElseThrow(() -> new NotFoundException("No se encontro la propuesta"));

//     PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
//         .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));

//     if (!propuesta.getPublicacion().getId().equals(publicacion.getId())) {
//       throw new BadInputException("La publicacion no corresponde a la propuesta");
//     }

//     if (!publicacion.getPublicante().equals(usuario)) {
//       throw new UnauthorizedException("El usuario no es el dueño de la publicacion");
//     }

//     if(!propuesta.getEstado().equals(EstadoPropuesta.PENDIENTE)) {
//       throw new ConflictException("La propuesta ya fue aceptada o rechazada");
//     }

//     propuesta.rechazar();
//     FiguritaColeccion figuritaColeccion = propuesta.getPublicacion().getFiguritaColeccion();
//     figuritaColeccion.reducirCantidadOfertada();

//     propuestasIntercambioRepository.save(propuesta);
//   }

//   /**
//    * Acepta una propuesta de intercambio pendiente y ejecuta la transferencia de figuritas.
//    * Al aceptar: reduce el stock de la figurita publicada, agrega las figuritas ofrecidas
//    * a la colección del publicante, elimina la figurita de sus faltantes si corresponde,
//    * cierra la publicación cuando el stock llega a cero y rechaza automáticamente el resto
//    * de propuestas pendientes.
//    *
//    * @param publicacionId identificador de la publicación de intercambio
//    * @param propuestaId   identificador de la propuesta a aceptar
//    * @param userId        identificador del usuario dueño de la publicación
//    * @throws UserNotFoundException   si el usuario no existe
//    * @throws NotFoundException       si la propuesta o la publicación no existen
//    * @throws BadInputException       si la propuesta no corresponde a la publicación indicada
//    * @throws UnauthorizedException   si el usuario no es el dueño de la publicación
//    * @throws ConflictException       si la propuesta ya fue aceptada o rechazada previamente
//    */
//   @Transactional
//   public void aceptarPropuesta(Integer publicacionId, Integer propuestaId, Integer userId) {
//     Usuario usuario = usuariosRepository.findById(userId)
//         .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

//     PropuestaIntercambio propuesta = propuestasIntercambioRepository.findById(propuestaId)
//         .orElseThrow(() -> new NotFoundException("No se encontro la propuesta"));

//     PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
//         .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));

//     if (!propuesta.getPublicacion().getId().equals(publicacion.getId())) {
//       throw new BadInputException("La publicacion no corresponde a la propuesta");
//     }

//     if (!publicacion.getPublicante().equals(usuario)) {
//       throw new UnauthorizedException("El usuario no es el dueño de la publicacion");
//     }

//     if(!propuesta.getEstado().equals(EstadoPropuesta.PENDIENTE)) {
//       throw new ConflictException("La propuesta ya fue aceptada o rechazada");
//     }

//     propuesta.aceptar();
//     publicacion.setPropuestaAceptada(propuesta);
//     publicacion.getFiguritaColeccion().reducirCantidad();
//     //verificar figuritas de propuestas y agregar a coleccion
//     propuesta.getFiguritas().forEach(figu -> {
//       Optional<FiguritaColeccion> figuritaOptional = usuario.getRepetidas().stream().filter(
//           f -> f.getFigurita().getNumero().equals(figu.getNumero())
//       ).findFirst();

//       if (figuritaOptional.isPresent()) {
//         FiguritaColeccion figuritaColeccion = figuritaOptional.get();
//         figuritaColeccion.aumentarCantidad();
//       }

//       if (usuario.getFaltantes().contains(figu)) {
//         usuario.getFaltantes().remove(figu);
//       }

//       //eliminar figus de coleccion de usuario que oferto
//       propuesta.getUsuario().getRepetidas().forEach(
//           repetida -> {
//             if (repetida.getFigurita().getNumero().equals(figu.getNumero())) {
//               repetida.reducirCantidad();
//               repetida.reducirCantidadOfertada();
//             }
//           }
//       );
//     });


//     if (publicacion.getFiguritaColeccion().getCantidad() == 0){
//       publicacion.setEstado(EstadoPublicacion.FINALIZADA);
//     }



//     propuestasIntercambioRepository.save(propuesta);
//     publicacionesIntercambioRepository.save(publicacion);
//     //rechazo las otras propuestas
//     //se deberia realizar luego de responder al user
//     List<PropuestaIntercambio> propuestas = propuestasIntercambioRepository.findByPublicacionId(publicacionId);
//     propuestas.stream().filter(p -> !p.getId().equals(propuestaId)).forEach(p -> p.rechazar());
//     //figurita coleccion es de usuario
//     propuestasIntercambioRepository.saveAll(propuestas);
//   }

//   /**
//    * Busca publicaciones de intercambio activas aplicando filtros opcionales y devuelve el resultado paginado.
//    *
//    * @param seleccion     filtra por nombre de selección (subcadena, puede ser {@code null})
//    * @param nombreJugador filtra por nombre de jugador (subcadena, puede ser {@code null})
//    * @param equipo        filtra por nombre de equipo (subcadena, puede ser {@code null})
//    * @param categoria     filtra por categoría de la figurita (puede ser {@code null})
//    * @param page          número de página solicitada (1-based)
//    * @param per_page      cantidad de resultados por página
//    * @return {@link PaginacionDto} con la lista de {@link PublicacionDto} correspondiente a la página solicitada
//    */
//   public PaginacionDto<PublicacionDto> buscarPublicaciones(
//       String seleccion, String nombreJugador, String equipo, Categoria categoria, Integer page, Integer per_page
//   ) {
//     //primero obtener publicaciones activas
//     List<PublicacionIntercambio> publicaciones = publicacionesIntercambioRepository.findByEstado(EstadoPublicacion.ACTIVA);

//     //podria dejar el filtrado en otra seccion del codigo para no cargar mucho el service

//     if(!publicaciones.isEmpty()) {
//       //luego reemplazar por filtros de mongodb

//       if(seleccion != null) {
//         publicaciones = publicaciones.stream().filter(publicacion ->
//         publicacion.getFiguritaColeccion().getFigurita().getSeleccion().contains(seleccion)
//         ).toList();
//       }

//       if(nombreJugador != null) {
//         publicaciones = publicaciones.stream().filter(publicacion ->
//             publicacion.getFiguritaColeccion().getFigurita().getJugador().contains(seleccion)
//         ).toList();
//       }

//       if(equipo != null) {
//         publicaciones = publicaciones.stream().filter(publicacion ->
//             publicacion.getFiguritaColeccion().getFigurita().getEquipo().contains(equipo)
//         ).toList();
//       }

//       if (categoria != null) {
//         publicaciones = publicaciones.stream().filter(publicacion ->
//             publicacion.getFiguritaColeccion().getFigurita().getCategoria().equals(categoria)
//         ).toList();
//       }
//       //verificar num paginas y armar resultado paginado
//       int totalPages = (int) Math.ceil((double) publicaciones.size() / per_page);
//       int startIndex = (page - 1) * per_page;
//       int endIndex = Math.min(startIndex + per_page, publicaciones.size());
//       List<PublicacionIntercambio> paginatedPublicaciones = publicaciones.subList(startIndex, endIndex);
//       //map publicaciones to dtos
//       List<PublicacionDto> mapeados = publicacionMapper.mapToDto(paginatedPublicaciones);

//       return new PaginacionDto<>(mapeados, page, totalPages);
//     } else {
//       return new PaginacionDto<>(null, 1, 1);
//     }

//   }
// }
