package com.tacs.tp1c2026.entities.dto.mappers;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.output.BestOfferDto;
import com.tacs.tp1c2026.entities.dto.card.output.CardDTO;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.Category;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuctionMapper {
  public AuctionDto mapAuction(Auction auction){
    BestOfferDto bestOfferDto = null;
    if (auction.getBestOffer() != null) {
      List<CardDTO> offeredCards;
      offeredCards = auction.getBestOffer().getOfferedItems().stream()
          .map(item -> new CardDTO(
              item.getCard().getNumber(),
              item.getCard().getPlayer(),
              item.getCard().getCountry(),
              item.getCard().getTeam(),
              item.getCard().getDescription(),
              item.getCard().getCategory())).toList();

      bestOfferDto = new BestOfferDto();
      bestOfferDto.setUsername(auction.getBestOffer().getBidder().getName());
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
