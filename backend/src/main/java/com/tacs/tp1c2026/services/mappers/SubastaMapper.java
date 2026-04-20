package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.ItemOfertaSubasta;
import com.tacs.tp1c2026.entities.OfertaSubasta;
import com.tacs.tp1c2026.entities.Subasta;
import com.tacs.tp1c2026.entities.dto.output.OfertaSubastaDto;
import com.tacs.tp1c2026.entities.dto.output.SubastaDto;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades de subasta a sus representaciones DTO.
 */
@Component
public class SubastaMapper {

  /**
   * Convierte una {@link Subasta} a su representación DTO.
   *
   * @param subasta entidad de subasta
   * @return {@link SubastaDto} con las propiedades principales de la subasta
   */
  public SubastaDto mapSubasta(Subasta subasta) {
    return new SubastaDto(
            subasta.getId(),
            subasta.getUsuarioPublicante() != null ? subasta.getUsuarioPublicante().getId() : null,
            subasta.getFigurita() != null ? subasta.getFigurita().getNumero() : null,
            subasta.getCantidadMinFiguritas(),
            subasta.getFechaCreacion(),
            subasta.getFechaCierre(),
            subasta.getEstado() != null ? subasta.getEstado().name() : null
    );
  }

  /**
   * Convierte una {@link OfertaSubasta} a su representación DTO.
   *
   * @param ofertaSubasta entidad de oferta de subasta
   * @return {@link OfertaSubastaDto} con el estado, figuritas ofrecidas y referencias asociadas
   */
  public OfertaSubastaDto mapOfertaSubasta(OfertaSubasta ofertaSubasta) {
    List<ItemOfertaSubasta> items = ofertaSubasta.getItemsOfrecidos() == null ? List.of() : ofertaSubasta.getItemsOfrecidos();

    Integer cantidadTotal = items.stream()
        .filter(Objects::nonNull)
        .map(ItemOfertaSubasta::getCantidad)
        .filter(Objects::nonNull)
        .reduce(0, Integer::sum);

    List<Integer> idsFiguritas = items.stream()
        .filter(Objects::nonNull)
        .filter(i -> i.getFigurita() != null)
        .map(i -> i.getFigurita().getId())
        .filter(Objects::nonNull)
        .toList();

    List<OfertaSubastaDto.ItemOfertaDetalleDto> itemsDetalle = items.stream()
        .filter(Objects::nonNull)
        .filter(i -> i.getFigurita() != null)
        .map(i -> new OfertaSubastaDto.ItemOfertaDetalleDto(
            i.getFigurita().getId(),
            i.getFigurita().getNumero(),
            i.getCantidad()
        ))
        .toList();

    return new OfertaSubastaDto(
        ofertaSubasta.getId(),
        ofertaSubasta.getSubasta() != null ? ofertaSubasta.getSubasta().getId() : null,
        ofertaSubasta.getUsuarioPostor() != null ? ofertaSubasta.getUsuarioPostor().getId() : null,
        cantidadTotal,
        idsFiguritas,
        itemsDetalle,
        ofertaSubasta.getEstado().name()
    );
  }
}

