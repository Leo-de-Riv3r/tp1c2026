package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.OfertaSubasta;
import com.tacs.tp1c2026.entities.PropuestaIntercambio;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.Subasta;
import com.tacs.tp1c2026.entities.dto.output.OfertaSubastaDto;
import com.tacs.tp1c2026.entities.dto.output.PropuestaIntercambioDto;
import com.tacs.tp1c2026.entities.dto.output.PublicacionIntercambioDto;
import com.tacs.tp1c2026.entities.dto.output.SubastaDto;
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

//	  public OperacionesUsuarioDto obtenerOperaciones(Integer userId) {
//			validarUsuarioExiste(userId);
//
//			OperacionesUsuarioDto dto = new OperacionesUsuarioDto();
//			dto.setPublicacionesIntercambioPropias(obtenerPublicacionesPropias(userId));
//			dto.setPropuestasEnviadas(obtenerPropuestasEnviadas(userId));
//			dto.setPropuestasRecibidas(obtenerPropuestasRecibidas(userId));
//			dto.setSubastasActivas(obtenerSubastasActivas(userId));
//			dto.setOfertasSubastaEnviadas(obtenerOfertasSubastaEnviadas(userId));
//			return dto;
//	  }

      /*publicaciones de INTERCAMBIO de figus propias del usuario */
	  public List<PublicacionIntercambioDto> obtenerPublicacionesPropias(Integer userId) {
			validarUsuarioExiste(userId);
			return publicacionesIntercambioRepository.findByPublicanteId(userId).stream().map(this::mapPublicacion).toList();
	  }


	// PROPUESTAS de INTERCAMBIO de figus ENVIADAS por el usuario las mapea para enviarla al front
	  public List<PropuestaIntercambioDto> obtenerPropuestasEnviadas(Integer userId) {
			validarUsuarioExiste(userId);
			return propuestasIntercambioRepository.findByUsuarioId(userId).stream().map(this::mapPropuesta).toList();
	  }


	// PROPUESTAS de INTERCAMBIO de figus RECIBIDAS
	  public List<PropuestaIntercambioDto> obtenerPropuestasRecibidas(Integer userId) {
			validarUsuarioExiste(userId);
			return propuestasIntercambioRepository.findByPublicacionPublicanteId(userId).stream().map(this::mapPropuesta).toList();
	  }


	  public List<SubastaDto> obtenerSubastasActivas(Integer userId) {
			validarUsuarioExiste(userId);
			return subastaRepository.findByUsuarioPublicanteIdAndEstado(userId, EstadoSubasta.ACTIVA).stream().map(this::mapSubasta).toList();
	  }


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


	  public List<OfertaSubastaDto> obtenerOfertasSubastaEnviadas(Integer userId) {
			validarUsuarioExiste(userId);
			return ofertasSubastaRepository.findByUsuarioPostorId(userId).stream().map(this::mapOfertaSubasta).toList();
	  }


	  public List<OfertaSubastaDto> obtenerOfertasSubastaRecibidas(Integer userId) {
			validarUsuarioExiste(userId);
			return ofertasSubastaRepository.findBySubastaUsuarioPublicanteId(userId).stream().map(this::mapOfertaSubasta).toList();
	  }





	  private void validarUsuarioExiste(Integer userId) {
			usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
	  }

	  private PublicacionIntercambioDto mapPublicacion(PublicacionIntercambio publicacion) {

		  PublicacionIntercambioDto dto = new PublicacionIntercambioDto(); // creo el dto vacio
		  dto.setPublicacionId(publicacion.getId()); // seteo el id de la publicacion
		  dto.setEstado(publicacion.getEstado().name()); // seteo
		  if (publicacion.getFiguritaColeccion() != null && publicacion.getFiguritaColeccion().getFigurita() != null) {
			  dto.setNumFiguritaPublicada(publicacion.getFiguritaColeccion().getFigurita().getNumero());
		  }
		  return dto;
	  }

	  private PropuestaIntercambioDto mapPropuesta(PropuestaIntercambio propuesta) {
		PropuestaIntercambioDto dto = new PropuestaIntercambioDto();
		List<Figurita> figuritasOfrecidas = propuesta.getFiguritas() == null ? List.of() : propuesta.getFiguritas(); // obtengo las figuritas de la propuesta
		dto.setPropuestaId(propuesta.getId());
		dto.setEstado(propuesta.getEstado().name());
		dto.setCantidadFiguritasOfrecidas(figuritasOfrecidas.size());
		dto.setNumerosFiguritasOfrecidas(figuritasOfrecidas.stream()
				.filter(Objects::nonNull)
				.map(Figurita::getNumero)
				.filter(Objects::nonNull)
				.toList());

		if (propuesta.getUsuario() != null) {
		  dto.setUsuarioId(propuesta.getUsuario().getId());
		}
		if (propuesta.getPublicacion() != null) {
		  dto.setPublicacionId(propuesta.getPublicacion().getId());
		  if (propuesta.getPublicacion().getFiguritaColeccion() != null && propuesta.getPublicacion().getFiguritaColeccion().getFigurita() != null) {
			dto.setNumFiguritaPublicada(propuesta.getPublicacion().getFiguritaColeccion().getFigurita().getNumero());
		  }
		}
		return dto;
	  }

	  private SubastaDto mapSubasta(Subasta subasta) {
		SubastaDto dto = new SubastaDto();
		dto.setSubastaId(subasta.getId());
		if (subasta.getUsuarioPublicante() != null) {
		  dto.setUsuarioPublicanteId(subasta.getUsuarioPublicante().getId());
		}
		dto.setCantidadMinFiguritas(subasta.getCantidadMinFiguritas());
		dto.setFechaCreacion(subasta.getFechaCreacion());
		dto.setFechaCierre(subasta.getFechaCierre());
		dto.setEstado(subasta.getEstado().name());
		if (subasta.getFiguritaPublicada() != null && subasta.getFiguritaPublicada().getFigurita() != null) {
		  dto.setNumFiguritaPublicada(subasta.getFiguritaPublicada().getFigurita().getNumero());
		}
		return dto;
	  }
	  // mapeo
	  private OfertaSubastaDto mapOfertaSubasta(OfertaSubasta ofertaSubasta) {
			OfertaSubastaDto dto = new OfertaSubastaDto();
			dto.setOfertaId(ofertaSubasta.getId());
			dto.setEstado(ofertaSubasta.getEstado().name());
			if (ofertaSubasta.getUsuarioPostor() != null) {
			  dto.setUsuarioPostorId(ofertaSubasta.getUsuarioPostor().getId());
			}
			List<Figurita> figuritas = ofertaSubasta.getFiguritasOfrecidas();
			dto.setCantidadFiguritasOfrecidas(figuritas == null ? 0 : figuritas.size());
			dto.setNumerosFiguritasOfrecidas(figuritas == null ? List.of() : figuritas.stream()
					.filter(Objects::nonNull)
					.map(Figurita::getNumero)
					.filter(Objects::nonNull)
					.toList());
			if (ofertaSubasta.getSubasta() != null) {
			  dto.setSubastaId(ofertaSubasta.getSubasta().getId());
			}
			return dto;
	  }

}
