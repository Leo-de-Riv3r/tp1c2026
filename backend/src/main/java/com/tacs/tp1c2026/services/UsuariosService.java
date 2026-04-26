//package com.tacs.tp1c2026.services;
//
//import com.tacs.tp1c2026.entities.*;
//import com.tacs.tp1c2026.entities.dto.output.*;
//import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
//import com.tacs.tp1c2026.exceptions.BadInputException;
//import com.tacs.tp1c2026.exceptions.UserNotFoundException;
//import com.tacs.tp1c2026.repositories.OfertasSubastaRepository;
//import com.tacs.tp1c2026.repositories.PropuestasIntercambioRepository;
//import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
//import com.tacs.tp1c2026.repositories.SubastaRepository;
//import com.tacs.tp1c2026.repositories.UsuariosRepository;
//import com.tacs.tp1c2026.services.mappers.SubastaMapper;
//import com.tacs.tp1c2026.services.mappers.IntercambioMapper;
//import java.util.List;
//import java.util.Objects;
//import org.springframework.stereotype.Service;
//
//@Service
//public class UsuariosService {
//
//	  private final UsuariosRepository usuariosRepository;
//	  private final PublicacionesIntercambioRepository publicacionesIntercambioRepository;
//	  private final PropuestasIntercambioRepository propuestasIntercambioRepository;
//	  private final SubastaRepository subastaRepository;
//	  private final OfertasSubastaRepository ofertasSubastaRepository;
//	  private final SubastaMapper subastaMapper;
//	  private final IntercambioMapper intercambioMapper;
//
//	  public UsuariosService(
//			  UsuariosRepository usuariosRepository,
//			  PublicacionesIntercambioRepository publicacionesIntercambioRepository,
//			  PropuestasIntercambioRepository propuestasIntercambioRepository,
//			  SubastaRepository subastaRepository,
//			  OfertasSubastaRepository ofertasSubastaRepository,
//			  SubastaMapper subastaMapper,
//			  IntercambioMapper intercambioMapper) {
//		  this.usuariosRepository = usuariosRepository;
//		  this.publicacionesIntercambioRepository = publicacionesIntercambioRepository;
//		  this.propuestasIntercambioRepository = propuestasIntercambioRepository;
//		  this.subastaRepository = subastaRepository;
//		  this.ofertasSubastaRepository = ofertasSubastaRepository;
//		  this.subastaMapper = subastaMapper;
//		  this.intercambioMapper = intercambioMapper;
//	  }
//
//	public Usuario crearUsuario(String nombre) {
//		Usuario usuario = new Usuario();
//		usuario.setNombre(nombre);
//		usuario.setFechaAlta(java.time.LocalDateTime.now());
//		return usuariosRepository.save(usuario);
//	}
//
//	public List<UsuarioDto> listarUsuarios() {
//		return usuariosRepository.findAll().stream().map(u -> {
//			UsuarioDto dto = new UsuarioDto();
//			dto.setId(u.getId());
//			dto.setNombre(u.getNombre());
//			dto.setFechaAlta(u.getFechaAlta());
//			return dto;
//		}).toList();
//	}
//
//      /*publicaciones de INTERCAMBIO de figus propias del usuario */
//  /**
//   * Retorna las publicaciones de intercambio creadas por el usuario.
//   *
//   * @param userId identificador del usuario
//   * @return lista de {@link PublicacionIntercambioDto} con las publicaciones del usuario
//   * @throws UserNotFoundException si el usuario no existe
//   */
//	  public List<PublicacionIntercambioDto> obtenerPublicacionesPropias(Integer userId) {
//		  validarUsuarioExiste(userId);
//		  return publicacionesIntercambioRepository.findByPublicanteId(userId).stream().map(intercambioMapper::mapPublicacion).toList();
//	  }
//
//
//	// PROPUESTAS de INTERCAMBIO de figus ENVIADAS por el usuario las mapea para enviarla al front
//  /**
//   * Retorna las propuestas de intercambio enviadas por el usuario a publicaciones ajenas.
//   *
//   * @param userId identificador del usuario
//   * @return lista de {@link PropuestaIntercambioDto} con las propuestas enviadas
//   * @throws UserNotFoundException si el usuario no existe
//   */
//	  public List<PropuestaIntercambioDto> obtenerPropuestasEnviadas(Integer userId) {
//		  validarUsuarioExiste(userId);
//		  return propuestasIntercambioRepository.findByUsuarioId(userId).stream().map(intercambioMapper::mapPropuesta).toList();
//	  }
//
//
//	// PROPUESTAS de INTERCAMBIO de figus RECIBIDAS
//  /**
//   * Retorna las propuestas de intercambio recibidas en las publicaciones propias del usuario.
//   *
//   * @param userId identificador del usuario
//   * @return lista de {@link PropuestaIntercambioDto} con las propuestas recibidas
//   * @throws UserNotFoundException si el usuario no existe
//   */
//	  public List<PropuestaIntercambioDto> obtenerPropuestasRecibidas(Integer userId) {
//		  validarUsuarioExiste(userId);
//		  return propuestasIntercambioRepository.findByPublicacionPublicanteId(userId).stream().map(intercambioMapper::mapPropuesta).toList();
//	  }
//
//
////  /**
////   * Retorna las subastas en estado ACTIVA creadas por el usuario.
////   *
////   * @param userId identificador del usuario
////   * @return lista de {@link SubastaDto} con las subastas activas del usuario
////   * @throws UserNotFoundException si el usuario no existe
////   */
////	  public List<SubastaDto> obtenerSubastasActivas(Integer userId) {
////			validarUsuarioExiste(userId);
////			return subastaRepository.findByUsuarioPublicanteIdAndEstado(userId, EstadoSubasta.ACTIVA).stream().map(this::mapSubasta).toList();
////	  }
//
//
//  /**
//   * Retorna las subastas del usuario filtradas por estado.
//   * Si el parámetro {@code estado} es nulo o vacío, devuelve todas las subastas del usuario.
//   *
//   * @param userId identificador del usuario
//   * @param estado nombre del estado ({@link EstadoSubasta}) a filtrar; {@code null} o vacío devuelve todas
//   * @return lista de {@link SubastaDto} correspondientes al criterio de búsqueda
//   * @throws UserNotFoundException si el usuario no existe
//   * @throws BadInputException     si el valor de {@code estado} no es válido
//   */
//	  public List<SubastaDto> obtenerSubastasPorEstado(Integer userId, String estado) {
//		  validarUsuarioExiste(userId);
//
//		  if (estado == null || estado.isBlank()) {
//		    return subastaRepository.findByUsuarioPublicanteId(userId).stream().map(subastaMapper::mapSubasta).toList();
//		  }
//
//		  try {
//		    EstadoSubasta estadoSubasta = EstadoSubasta.valueOf(estado.trim().toUpperCase());
//		    return subastaRepository.findByUsuarioPublicanteIdAndEstado(userId, estadoSubasta).stream().map(subastaMapper::mapSubasta).toList();
//		  } catch (IllegalArgumentException e) {
//		    throw new BadInputException("Estado de subasta invalido");
//		  }
//	  }
//
//
//  /**
//   * Retorna las ofertas de subasta enviadas por el usuario a subastas ajenas.
//   *
//   * @param userId identificador del usuario postor
//   * @return lista de {@link OfertaSubastaDto} con las ofertas enviadas
//   * @throws UserNotFoundException si el usuario no existe
//   */
//	  public List<OfertaSubastaDto> obtenerOfertasSubastaEnviadas(Integer userId) {
//		  validarUsuarioExiste(userId);
//		  return ofertasSubastaRepository.findByUsuarioPostorId(userId).stream().map(subastaMapper::mapOfertaSubasta).toList();
//	  }
//
//
//  /**
//   * Retorna las ofertas de subasta recibidas en las subastas creadas por el usuario.
//   *
//   * @param userId identificador del usuario publicante
//   * @return lista de {@link OfertaSubastaDto} con las ofertas recibidas
//   * @throws UserNotFoundException si el usuario no existe
//   */
//	  public List<OfertaSubastaDto> obtenerOfertasSubastaRecibidas(Integer userId) {
//		  validarUsuarioExiste(userId);
// 		  return ofertasSubastaRepository.findByAuctionUsuarioPublicanteId(userId).stream().map(subastaMapper::mapOfertaSubasta).toList();
//	  }
//
//
//
//
//
//  /**
//   * Valida que el usuario con el identificador indicado exista en el repositorio.
//   *
//   * @param userId identificador del usuario a validar
//   * @throws UserNotFoundException si el usuario no existe
//   */
//	  private void validarUsuarioExiste(Integer userId) {
//		  usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
//	  }
//
//}
//
