package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.Sticker;
import com.tacs.tp1c2026.entities.TradeProposal;
import com.tacs.tp1c2026.entities.TradePublication;
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
   * Convierte una {@link TradePublication} a su representación DTO.
   *
   * @param publicacion entidad de publicación de intercambio
   * @return {@link PublicacionIntercambioDto} con los datos básicos de la publicación
   */
  public PublicacionIntercambioDto mapPublicacion(TradePublication publicacion) {
    Integer stickerNumber = null;
    if (publicacion.getColeccion() != null && publicacion.getColeccion().getFigurita() != null) {
      stickerNumber = publicacion.getColeccion().getFigurita().getNumber();
    }
    return new PublicacionIntercambioDto(
        publicacion.getId(),
        stickerNumber,
        publicacion.getEstado().name()
    );
  }

  /**
   * Convierte una {@link TradeProposal} a su representación DTO.
   *
   * @param propuesta entidad de propuesta de intercambio
   * @return {@link PropuestaIntercambioDto} con el estado, figuritas ofrecidas y referencias asociadas
   */
  public PropuestaIntercambioDto mapPropuesta(TradeProposal propuesta) {
    List<Sticker> offeredStickers = propuesta.getFiguritas() == null ? List.of() : propuesta.getFiguritas();

    Integer publicationId = null;
    Integer publishedStickerNumber = null;
    if (propuesta.getPublicacion() != null) {
      publicationId = propuesta.getPublicacion().getId();
      if (propuesta.getPublicacion().getColeccion() != null && propuesta.getPublicacion().getColeccion().getFigurita() != null) {
        publishedStickerNumber = propuesta.getPublicacion().getColeccion().getFigurita().getNumber();
      }
    }

    return new PropuestaIntercambioDto(
        propuesta.getId(),
        publicationId,
        publishedStickerNumber,
        offeredStickers.size(),
        offeredStickers.stream()
            .filter(Objects::nonNull)
            .map(Sticker::getNumber)
            .filter(Objects::nonNull)
            .toList(),
        propuesta.getUsuario() != null ? propuesta.getUsuario().getId() : null,
        propuesta.getEstado().name()
    );
  }
}

