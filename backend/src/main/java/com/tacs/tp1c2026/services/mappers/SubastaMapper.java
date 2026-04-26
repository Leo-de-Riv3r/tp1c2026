package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.AuctionOfferItems;
import com.tacs.tp1c2026.entities.AuctionOffer;
import com.tacs.tp1c2026.entities.Auction;
import com.tacs.tp1c2026.entities.dto.output.AuctionOfferDto;
import com.tacs.tp1c2026.entities.dto.output.AuctionDTO;
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
   * @return {@link AuctionDTO} con las propiedades principales de la subasta
   */
  public AuctionDTO mapSubasta(Auction auction) {
    return new AuctionDTO(
            auction.getId(),
            auction.getUsuarioPublicanteId() != null
                ? auction.getUsuarioPublicanteId()
                : (auction.getUsuarioPublicante() != null ? auction.getUsuarioPublicante().getId() : null),
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
   * @param auctionOffer entidad de oferta de subasta
   * @return {@link AuctionOfferDto} con el estado, figuritas ofrecidas y referencias asociadas
   */
  public AuctionOfferDto mapOfertaSubasta(AuctionOffer auctionOffer) {
    List<AuctionOfferItems> items = auctionOffer.getItemsOfrecidos() == null ? List.of() : auctionOffer.getItemsOfrecidos();

    Integer cantidadTotal = items.stream()
        .filter(Objects::nonNull)
        .map(AuctionOfferItems::getCantidad)
        .filter(Objects::nonNull)
        .reduce(0, Integer::sum);

    List<Integer> idsFiguritas = items.stream()
        .filter(Objects::nonNull)
        .filter(i -> i.getFigurita() != null)
        .map(i -> i.getFigurita().getId())
        .filter(Objects::nonNull)
        .toList();

    List<AuctionOfferDto.OfferItemDetailDto> itemsDetalle = items.stream()
        .filter(Objects::nonNull)
        .filter(i -> i.getFigurita() != null)
        .map(i -> new AuctionOfferDto.OfferItemDetailDto(
            i.getFigurita().getId(),
            i.getFigurita().getNumber(),
            i.getCantidad()
        ))
        .toList();

    return new AuctionOfferDto(
        auctionOffer.getId(),
        auctionOffer.getAuction() != null ? auctionOffer.getAuction().getId() : null,
        auctionOffer.getUsuarioPostor() != null ? auctionOffer.getUsuarioPostor().getId() : null,
        cantidadTotal,
        idsFiguritas,
        itemsDetalle,
        auctionOffer.getEstado().name()
    );
  }
}

