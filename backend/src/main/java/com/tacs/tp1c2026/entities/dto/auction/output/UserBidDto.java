package com.tacs.tp1c2026.entities.dto.auction.output;

import com.tacs.tp1c2026.entities.enums.AuctionOfferStatus;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista plana para "Mis Ofertas": una entrada por oferta hecha por el usuario,
 * con el contexto de la subasta a la que pertenece.
 */
@Getter
@AllArgsConstructor
public class UserBidDto {
  private String auctionId;
  // Card publicada en la subasta
  private Integer cardNumber;
  private String cardDescription;
  private String cardCountry;
  private String cardTeam;
  // Publisher de la subasta
  private String publisherUserId;
  private String publisherName;
  // Estado de la subasta
  private AuctionStatus auctionStatus;
  private LocalDateTime closeDate;
  // Datos de la oferta del usuario
  private String offerId;
  private List<OfferItemDto> offeredItems;
  private AuctionOfferStatus offerStatus;
  private LocalDateTime bidDate;

  @Getter
  @AllArgsConstructor
  public static class OfferItemDto {
    private String cardId;
    private Integer cardNumber;
    private String cardDescription;
    private Integer amount;
  }
}
