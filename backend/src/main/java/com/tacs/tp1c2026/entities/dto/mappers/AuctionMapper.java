package com.tacs.tp1c2026.entities.dto.mappers;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionOfferDto;
import com.tacs.tp1c2026.entities.dto.auction.output.BestOfferDto;
import com.tacs.tp1c2026.entities.dto.card.output.CardDTO;
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

    List<AuctionOfferDto> offerDtos = auction.getOffers() == null
        ? List.of()
        : auction.getOffers().stream().map(o -> mapOffer(auction.getId(), o)).toList();

    return new AuctionDto(
        auction.getId(),
        auction.getCardNumber(),
        auction.getCardDescription(),
        auction.getCardCountry(),
        auction.getCardTeam(),
        auction.getCardCategory(),
        auction.getCloseDate(),
        auction.getStatus(),
        bestOfferDto,
        auction.getPublisherUser() != null ? auction.getPublisherUser().getId() : null,
        auction.getPublisherName(),
        auction.getPublisherAvatarId(),
        offerDtos
    );
  }

  private AuctionOfferDto mapOffer(String auctionId, AuctionOffer offer) {
    List<AuctionOfferDto.ItemDetailDto> items = offer.getOfferedItems() == null
        ? List.of()
        : offer.getOfferedItems().stream()
            .map(it -> new AuctionOfferDto.ItemDetailDto(
                it.getCard() != null ? it.getCard().getId() : null,
                it.getCard() != null ? it.getCard().getNumber() : null,
                it.getCard() != null ? it.getCard().getDescription() : null,
                it.getAmount()))
            .toList();
    return new AuctionOfferDto(
        offer.getId(),
        auctionId,
        offer.getBidderId(),
        offer.getBidderName(),
        items,
        offer.getStatus(),
        offer.getBidDate()
    );
  }

  public List<AuctionDto> mapAuctions(List<Auction> auctions) {
    return auctions.stream().map(this::mapAuction).toList();
  }
}
