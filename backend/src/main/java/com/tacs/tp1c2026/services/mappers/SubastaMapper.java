package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.dto.output.auction.AuctionOfferDto;
import com.tacs.tp1c2026.entities.dto.output.SubastaDto;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades de subasta a sus representaciones DTO.
 */
@Component
public class SubastaMapper {

  /**
   * Convierte una {@link Auction} a su representación DTO.
   *
   * @param subasta entidad de subasta
   * @return {@link SubastaDto} con las propiedades principales de la subasta
   */
  public SubastaDto mapSubasta(Auction subasta) {
    return new SubastaDto(
            subasta.getId(),
            subasta.getPublisherUser() != null ? subasta.getPublisherUser().getId() : null,
            subasta.getCard() != null ? subasta.getCard().getNumber() : null,
            subasta.getMinCardsRequired(),
            subasta.getCreatedDate(),
            subasta.getFinishDate(),
            subasta.getState() != null ? subasta.getState() : null
    );
  }

  /**
   * Convierte una {@link AuctionOffer} a su representación DTO.
   *
   * @param ofertaSubasta entidad de oferta de subasta
   * @return {@link AuctionOfferDto} con el estado, figuritas ofrecidas y referencias asociadas
   */
//  public AuctionOfferDto mapOfertaSubasta(AuctionOffer ofertaSubasta) {
//    List<ItemOfertaSubasta> items = ofertaSubasta.getItemsOfrecidos() == null ? List.of() : ofertaSubasta.getItemsOfrecidos();
//
//    Integer cantidadTotal = items.stream()
//        .filter(Objects::nonNull)
//        .map(ItemOfertaSubasta::getCantidad)
//        .filter(Objects::nonNull)
//        .reduce(0, Integer::sum);
//
//    List<Integer> idsFiguritas = items.stream()
//        .filter(Objects::nonNull)
//        .filter(i -> i.getCard() != null)
//        .map(i -> i.getCard().getId())
//        .filter(Objects::nonNull)
//        .toList();
//
//    List<AuctionOfferDto.ItemOfertaDetalleDto> itemsDetalle = items.stream()
//        .filter(Objects::nonNull)
//        .filter(i -> i.getCard() != null)
//        .map(i -> new AuctionOfferDto.ItemOfertaDetalleDto(
//            i.getCard().getId(),
//            i.getCard().getNumber(),
//            i.getCantidad()
//        ))
//        .toList();
//
//    return new AuctionOfferDto(
//        ofertaSubasta.getId(),
//        ofertaSubasta.getAuction(),
//        ofertaSubasta.getUsuarioPostor() != null ? ofertaSubasta.getUsuarioPostor().getId() : ofertaSubasta.getUsuarioPostorId(),
//        cantidadTotal,
//        idsFiguritas,
//        itemsDetalle,
//        ofertaSubasta.getEstado().name()
//    );
//  }
}

