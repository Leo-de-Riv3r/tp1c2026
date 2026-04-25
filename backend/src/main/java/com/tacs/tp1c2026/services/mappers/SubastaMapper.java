package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.AuctionItem;
import com.tacs.tp1c2026.entities.AuctionOffer;
import com.tacs.tp1c2026.entities.Auction;
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
   * Convierte una {@link Auction} a su representación DTO.
   *
   * @param auction entidad de subasta
   * @return {@link SubastaDto} con las propiedades principales de la subasta
   */
  public SubastaDto mapSubasta(Auction auction) {
    return new SubastaDto(
            auction.getId(),
            auction.getUsuarioPublicante() != null ? auction.getUsuarioPublicante().getId() : null,
            auction.getFigurita() != null ? auction.getFigurita().getNumber() : null,
            auction.getCantidadMinFiguritas(),
            auction.getFechaCreacion(),
            auction.getFechaCierre(),
            auction.getEstado() != null ? auction.getEstado().name() : null
    );
  }

  /**
   * Convierte una {@link AuctionOffer} a su representación DTO.
   *
   * @param auctionoffer entidad de oferta de subasta
   * @return {@link OfertaSubastaDto} con el estado, figuritas ofrecidas y referencias asociadas
   */
  public OfertaSubastaDto mapOfertaSubasta(AuctionOffer auctionoffer) {
    List<AuctionItem> items = auctionoffer.getItemsOfrecidos() == null ? List.of() : auctionoffer.getItemsOfrecidos();

    Integer cantidadTotal = items.stream()
        .filter(Objects::nonNull)
        .map(AuctionItem::getAmount)
        .filter(Objects::nonNull)
        .reduce(0, Integer::sum);

    List<Integer> idsFiguritas = items.stream()
        .filter(Objects::nonNull)
        .filter(i -> i.getFigurita() != null)
        .map(i -> i.getFigurita().getId())
        .filter(Objects::nonNull)
        .toList();

    List<OfertaSubastaDto.ItemOfferDetailDto> itemsDetalle = items.stream()
        .filter(Objects::nonNull)
        .filter(i -> i.getFigurita() != null)
        .map(i -> new OfertaSubastaDto.ItemOfferDetailDto(
            i.getFigurita().getId(),
            i.getFigurita().getNumber(),
            i.getAmount()
        ))
        .toList();

    return new OfertaSubastaDto(
        auctionoffer.getId(),
        auctionoffer.getSubasta() != null ? auctionoffer.getSubasta().getId() : auctionoffer.getSubastaId(),
        auctionoffer.getUsuarioPostor() != null ? auctionoffer.getUsuarioPostor().getId() : auctionoffer.getUsuarioPostorId(),
        cantidadTotal,
        idsFiguritas,
        itemsDetalle,
        auctionoffer.getEstado().name()
    );
  }
}

