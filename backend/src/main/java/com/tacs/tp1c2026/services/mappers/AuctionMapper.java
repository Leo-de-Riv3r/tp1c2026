package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.Auction;
import com.tacs.tp1c2026.entities.dto.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.output.BestOfferDto;
import com.tacs.tp1c2026.entities.dto.output.CardDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuctionMapper {
  public AuctionDto mapAuction(Auction auction){
    BestOfferDto bestOfferDto = null;
    if (auction.getMejorOferta() != null) {
      List<CardDto> offeredCards;
      offeredCards = auction.getMejorOferta().getOfferedCards().stream()
          .map(card -> new CardDto(
              card.getId(),
              card.getName(),
              card.getTeam(),
              card.getCountry(),
              card.getCategory()
              )).toList();

      bestOfferDto = new BestOfferDto();
      bestOfferDto.setUsername(auction.getMejorOferta().getBidderUser().getName());
      bestOfferDto.setCards(offeredCards);
    }

    return new AuctionDto(
        auction.getId(),
        auction.getCardName(),
        auction.getCardTeam(),
        auction.getCardCountry(),
        auction.getCardCategory(),
        auction.getFinishDate(),
        auction.getMinCardsRequired(),
        bestOfferDto
    );


  }

  public List<AuctionDto> mapAuctions(List<Auction> auctions) {
    return auctions.stream().map(this::mapAuction).toList();
  }
}
