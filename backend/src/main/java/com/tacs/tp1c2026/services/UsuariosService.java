package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.*;
import com.tacs.tp1c2026.entities.dto.output.*;
import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.OfertasSubastaRepository;
import com.tacs.tp1c2026.repositories.PropuestasIntercambioRepository;
import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
import com.tacs.tp1c2026.repositories.SubastaRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class UsuariosService {

	  private final UsuariosRepository usuariosRepository;
	  private final PublicacionesIntercambioRepository publicacionesIntercambioRepository;
	  private final PropuestasIntercambioRepository propuestasIntercambioRepository;
	  private final SubastaRepository subastaRepository;
	  private final OfertasSubastaRepository ofertasSubastaRepository;

	  public UsuariosService(
			  UsuariosRepository usuariosRepository,
			  PublicacionesIntercambioRepository publicacionesIntercambioRepository,
			  PropuestasIntercambioRepository propuestasIntercambioRepository,
			  SubastaRepository subastaRepository,
			  OfertasSubastaRepository ofertasSubastaRepository) {
		  this.usuariosRepository = usuariosRepository;
		  this.publicacionesIntercambioRepository = publicacionesIntercambioRepository;
		  this.propuestasIntercambioRepository = propuestasIntercambioRepository;
		  this.subastaRepository = subastaRepository;
		  this.ofertasSubastaRepository = ofertasSubastaRepository;
	  }

	public Usuario crearUsuario(String nombre) {
		Usuario usuario = new Usuario();
		usuario.setNombre(nombre);
		usuario.setFechaAlta(java.time.LocalDateTime.now());
		return usuariosRepository.save(usuario);
	}

	public List<UsuarioDto> listarUsuarios() {
		return usuariosRepository.findAll().stream().map(u -> {
			return new UsuarioDto(
				u.getId(),
				u.getNombre(),
				u.getFechaAlta()
			);
		}).toList();
	}

      /*publicaciones de INTERCAMBIO de figus propias del usuario */
  /**
   * Retorna las publicaciones de intercambio creadas por el usuario.
   *
   * @param userId identificador del usuario
   * @return lista de {@link PublicacionIntercambioDto} con las publicaciones del usuario
   * @throws UserNotFoundException si el usuario no existe
   */
	  public List<PublicacionIntercambioDto> obtenerPublicacionesPropias(Integer userId) {
			validarUsuarioExiste(userId);
			return publicacionesIntercambioRepository.findByPublicanteId(userId).stream().map(this::mapPublicacion).toList();
	  }


	// PROPUESTAS de INTERCAMBIO de figus ENVIADAS por el usuario las mapea para enviarla al front
  /**
   * Retorna las propuestas de intercambio enviadas por el usuario a publicaciones ajenas.
   *
   * @param userId identificador del usuario
   * @return lista de {@link PropuestaIntercambioDto} con las propuestas enviadas
   * @throws UserNotFoundException si el usuario no existe
   */
	  public List<PropuestaIntercambioDto> obtenerPropuestasEnviadas(Integer userId) {
			validarUsuarioExiste(userId);
			return propuestasIntercambioRepository.findByUsuarioId(userId).stream().map(this::mapPropuesta).toList();
	  }


	// PROPUESTAS de INTERCAMBIO de figus RECIBIDAS
  /**
   * Retorna las propuestas de intercambio recibidas en las publicaciones propias del usuario.
   *
   * @param userId identificador del usuario
   * @return lista de {@link PropuestaIntercambioDto} con las propuestas recibidas
   * @throws UserNotFoundException si el usuario no existe
   */
	  public List<PropuestaIntercambioDto> obtenerPropuestasRecibidas(Integer userId) {
			validarUsuarioExiste(userId);
			return propuestasIntercambioRepository.findByPublicacionPublicanteId(userId).stream().map(this::mapPropuesta).toList();
	  }


//  /**
//   * Retorna las subastas en estado ACTIVA creadas por el usuario.
//   *
//   * @param userId identificador del usuario
//   * @return lista de {@link SubastaDto} con las subastas activas del usuario
//   * @throws UserNotFoundException si el usuario no existe
//   */
//	  public List<SubastaDto> obtenerSubastasActivas(Integer userId) {
//			validarUsuarioExiste(userId);
//			return subastaRepository.findByUsuarioPublicanteIdAndEstado(userId, EstadoSubasta.ACTIVA).stream().map(this::mapSubasta).toList();
//	  }


  /**
   * Retorna las subastas del usuario filtradas por estado.
   * Si el parámetro {@code estado} es nulo o vacío, devuelve todas las subastas del usuario.
   *
   * @param userId identificador del usuario
   * @param estado nombre del estado ({@link EstadoSubasta}) a filtrar; {@code null} o vacío devuelve todas
   * @return lista de {@link SubastaDto} correspondientes al criterio de búsqueda
   * @throws UserNotFoundException si el usuario no existe
   * @throws BadInputException     si el valor de {@code estado} no es válido
   */
	  public List<SubastaDto> obtenerSubastasPorEstado(Integer userId, String estado) {
			validarUsuarioExiste(userId);

			if (estado == null || estado.isBlank()) {
			  return subastaRepository.findByUsuarioPublicanteId(userId).stream().map(this::mapSubasta).toList();
			}

			try {
			  EstadoSubasta estadoSubasta = EstadoSubasta.valueOf(estado.trim().toUpperCase());
			  return subastaRepository.findByUsuarioPublicanteIdAndEstado(userId, estadoSubasta).stream().map(this::mapSubasta).toList();
			} catch (IllegalArgumentException e) {
			  throw new BadInputException("Estado de subasta invalido");
			}
	  }


  /**
   * Retorna las ofertas de subasta enviadas por el usuario a subastas ajenas.
   *
   * @param userId identificador del usuario postor
   * @return lista de {@link OfertaSubastaDto} con las ofertas enviadas
   * @throws UserNotFoundException si el usuario no existe
   */
	  public List<OfertaSubastaDto> obtenerOfertasSubastaEnviadas(Integer userId) {
			validarUsuarioExiste(userId);
			return ofertasSubastaRepository.findByUsuarioPostorId(userId).stream().map(this::mapOfertaSubasta).toList();
	  }


  /**
   * Retorna las ofertas de subasta recibidas en las subastas creadas por el usuario.
   *
   * @param userId identificador del usuario publicante
   * @return lista de {@link OfertaSubastaDto} con las ofertas recibidas
   * @throws UserNotFoundException si el usuario no existe
   */
	  public List<OfertaSubastaDto> obtenerOfertasSubastaRecibidas(Integer userId) {
			validarUsuarioExiste(userId);
			return ofertasSubastaRepository.findBySubastaUsuarioPublicanteId(userId).stream().map(this::mapOfertaSubasta).toList();
	  }





  /**
   * Valida que el usuario con el identificador indicado exista en el repositorio.
   *
   * @param userId identificador del usuario a validar
   * @throws UserNotFoundException si el usuario no existe
   */
	  private void validarUsuarioExiste(Integer userId) {
			usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
	  }

  /**
   * Convierte una {@link PublicacionIntercambio} a su representación DTO.
   *
   * @param publicacion entidad de publicación de intercambio
   * @return {@link PublicacionIntercambioDto} con los datos básicos de la publicación
   */
	  private PublicacionIntercambioDto mapPublicacion(PublicacionIntercambio publicacion) {
		  Integer numFiguritaPublicada = null;
		  if (publicacion.getFiguritaColeccion() != null && publicacion.getFiguritaColeccion().getFigurita() != null) {
			  numFiguritaPublicada = publicacion.getFiguritaColeccion().getFigurita().getNumero();
		  }

		  return new PublicacionIntercambioDto(
			  publicacion.getId(),
			  numFiguritaPublicada,
			  publicacion.getEstado().name()
		  );
	  }

  /**
   * Convierte una {@link PropuestaIntercambio} a su representación DTO.
   *
   * @param propuesta entidad de propuesta de intercambio
   * @return {@link PropuestaIntercambioDto} con el estado, figuritas ofrecidas y referencias asociadas
   */
	  private PropuestaIntercambioDto mapPropuesta(PropuestaIntercambio propuesta) {
		List<Figurita> figuritasOfrecidas = propuesta.getFiguritas() == null ? List.of() : propuesta.getFiguritas(); // obtengo las figuritas de la propuesta
		List<Integer> numerosFiguritasOfrecidas = figuritasOfrecidas.stream()
				.filter(Objects::nonNull)
				.map(Figurita::getNumero)
				.filter(Objects::nonNull)
				.toList();

		Integer usuarioId = null;
		Integer publicacionId = null;
		Integer numFiguritaPublicada = null;

		if (propuesta.getUsuario() != null) {
		  usuarioId = propuesta.getUsuario().getId();
		}
		if (propuesta.getPublicacion() != null) {
		  publicacionId = propuesta.getPublicacion().getId();
		  if (propuesta.getPublicacion().getFiguritaColeccion() != null && propuesta.getPublicacion().getFiguritaColeccion().getFigurita() != null) {
			numFiguritaPublicada = propuesta.getPublicacion().getFiguritaColeccion().getFigurita().getNumero();
		  }
		}

		return new PropuestaIntercambioDto(
			propuesta.getId(),
			publicacionId,
			numFiguritaPublicada,
			figuritasOfrecidas.size(),
			numerosFiguritasOfrecidas,
			usuarioId,
			propuesta.getEstado().name()
		);
	  }

  /**
   * Convierte una {@link Subasta} a su representación DTO.
   *
   * @param subasta entidad de subasta
   * @return {@link SubastaDto} con las propiedades principales de la subasta
   */
	  private SubastaDto mapSubasta(Subasta subasta) {
		Integer usuarioPublicanteId = subasta.getUsuarioPublicante() != null ? subasta.getUsuarioPublicante().getId() : null;
		Integer numFiguritaPublicada = null;
		if (subasta.getFiguritaPublicada() != null && subasta.getFiguritaPublicada().getFigurita() != null) {
		  numFiguritaPublicada = subasta.getFiguritaPublicada().getFigurita().getNumero();
		}

		return new SubastaDto(
			subasta.getId(),
			usuarioPublicanteId,
			numFiguritaPublicada,
			subasta.getCantidadMinFiguritas(),
			subasta.getFechaCreacion(),
			subasta.getFechaCierre(),
			subasta.getEstado().name()
		);
	  }
	  // mapeo
  /**
   * Convierte una {@link OfertaSubasta} a su representación DTO.
   *
   * @param ofertaSubasta entidad de oferta de subasta
   * @return {@link OfertaSubastaDto} con el estado, figuritas ofrecidas y referencias asociadas
   */
	  private OfertaSubastaDto mapOfertaSubasta(OfertaSubasta ofertaSubasta) {
			Integer usuarioPostorId = ofertaSubasta.getUsuarioPostor() != null ? ofertaSubasta.getUsuarioPostor().getId() : null;
			List<ItemOfertaSubasta> items = ofertaSubasta.getItemsOfrecidos() == null ? List.of() : ofertaSubasta.getItemsOfrecidos();
			Integer cantidadFiguritasOfrecidas = items.stream()
					.filter(Objects::nonNull)
					.map(ItemOfertaSubasta::getCantidad)
					.filter(Objects::nonNull)
					.reduce(0, Integer::sum);
			List<Integer> idsFiguritasOfrecidas = items.stream()
					.filter(Objects::nonNull)
					.filter(i -> i.getFigurita() != null)
					.map(i -> i.getFigurita().getId())
					.filter(Objects::nonNull)
					.toList();
//			dto.setNumerosFiguritasOfrecidas(items.stream()
//					.filter(Objects::nonNull)
//					.filter(i -> i.getFigurita() != null)
//					.map(i -> i.getFigurita().getNumero())
//					.filter(Objects::nonNull)
//					.toList());
			List<OfertaSubastaDto.ItemOfertaDetalleDto> itemsOfrecidos = items.stream()
					.filter(Objects::nonNull)
					.filter(i -> i.getFigurita() != null)
					.map(i -> new OfertaSubastaDto.ItemOfertaDetalleDto(
						i.getFigurita().getId(),
						i.getFigurita().getNumero(),
						i.getCantidad()
					))
					.toList();
			Integer subastaId = ofertaSubasta.getSubasta() != null ? ofertaSubasta.getSubasta().getId() : null;

			return new OfertaSubastaDto(
				ofertaSubasta.getId(),
				subastaId,
				usuarioPostorId,
				cantidadFiguritasOfrecidas,
				idsFiguritasOfrecidas,
				itemsOfrecidos,
				ofertaSubasta.getEstado().name()
			);
	  }
}
