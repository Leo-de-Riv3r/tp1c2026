package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionItem;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.output.BestOfferDto;
import com.tacs.tp1c2026.entities.dto.card.output.CardDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuctionMapper {

  public AuctionDto mapAuction(Auction auction) {
    BestOfferDto bestOfferDto = null;
    if (auction.getBestOffer() != null) {
      AuctionOffer best = auction.getBestOffer();
      List<CardDTO> offeredCards = best.getOfferedItems().stream()
          .map(AuctionItem::getCard)
          .map(card -> new CardDTO(
              card.getNumber(),
              card.getPlayer(),
              card.getCountry(),
              card.getTeam(),
              card.getDescription(),
              card.getCategory()
          ))
          .toList();

      bestOfferDto = new BestOfferDto();
      bestOfferDto.setUsername(best.getBidder() != null ? best.getBidder().getName() : null);
      bestOfferDto.setCards(offeredCards);
    }

    return new AuctionDto(
        auction.getId(),
        auction.getCardNumber(),
        auction.getCardDescription(),
        auction.getCardCountry(),
        auction.getCardTeam(),
        auction.getCardCategory(),
        auction.getCloseDate(),
        auction.getStatus(),
        bestOfferDto
    );
  }

  public List<AuctionDto> mapAuctions(List<Auction> auctions) {
    return auctions.stream().map(this::mapAuction).toList();
  }
}
