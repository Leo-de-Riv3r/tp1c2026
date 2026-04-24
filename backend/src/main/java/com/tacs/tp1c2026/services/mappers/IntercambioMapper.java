package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.PropuestaIntercambio;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.dto.output.PropuestaIntercambioDto;
import com.tacs.tp1c2026.entities.dto.output.PublicacionIntercambioDto;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades de intercambio a sus representaciones DTO.
 */
@Component
public class IntercambioMapper {

  /**
   * Convierte una {@link PublicacionIntercambio} a su representación DTO.
   *
   * @param publicacion entidad de publicación de intercambio
   * @return {@link PublicacionIntercambioDto} con los datos básicos de la publicación
   */
  public PublicacionIntercambioDto mapPublicacion(PublicacionIntercambio publicacion) {
    Integer numFigurita = null;
    if (publicacion.getFiguritaColeccion() != null) {
      numFigurita = publicacion.getFiguritaColeccion().getNumber();
    }
    return new PublicacionIntercambioDto(
        publicacion.getId(),
        numFigurita,
        publicacion.getEstado().name()
    );
  }

  /**
   * Convierte una {@link PropuestaIntercambio} a su representación DTO.
   *
   * @param propuesta entidad de propuesta de intercambio
   * @return {@link PropuestaIntercambioDto} con el estado, figuritas ofrecidas y referencias asociadas
   */
  public PropuestaIntercambioDto mapPropuesta(PropuestaIntercambio propuesta) {
    List<Figurita> figuritasOfrecidas = propuesta.getFiguritas() == null ? List.of() : propuesta.getFiguritas();

    Integer publicacionId = null;
    Integer numFiguritaPublicada = null;
    if (propuesta.getPublicacion() != null) {
      publicacionId = propuesta.getPublicacion().getId();
      if (propuesta.getPublicacion().getFiguritaColeccion() != null) {
        numFiguritaPublicada = propuesta.getPublicacion().getFiguritaColeccion().getNumber();
      }
    }

    return new PropuestaIntercambioDto(
        propuesta.getId(),
        publicacionId,
        numFiguritaPublicada,
        figuritasOfrecidas.size(),
        figuritasOfrecidas.stream()
            .filter(Objects::nonNull)
            .map(Figurita::getNumber)
            .filter(Objects::nonNull)
            .toList(),
        propuesta.getUsuarioId(),
        propuesta.getEstado().name()
    );
  }
}

