package com.tacs.tp1c2026.entities.dto.mappers;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalCardCount;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalCategory;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalExchanges;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalReputation;
import com.tacs.tp1c2026.entities.dto.auction.output.AuctionConditionOutputDto;
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
      AuctionOffer best = auction.getBestOffer();
      List<CardDTO> offeredCards = best.getOfferedItems().stream()
          .filter(item -> item.getCard() != null)
          .map(item -> new CardDTO(
              item.getCard().getNumber(),
              item.getCard().getPlayer(),
              item.getCard().getCountry(),
              item.getCard().getTeam(),
              item.getCard().getDescription(),
              item.getCard().getCategory())).toList();

      bestOfferDto = new BestOfferDto();
      // Snapshot denormalizado: el bidder es un @DocumentReference embebido que no hidrata.
      bestOfferDto.setUsername(best.getBidderName());
      bestOfferDto.setCards(offeredCards);
    }

    List<AuctionOfferDto> offerDtos = auction.getOffers() == null
        ? List.of()
        : auction.getOffers().stream().map(o -> mapOffer(auction.getId(), o)).toList();

    List<AuctionConditionOutputDto> conditionDtos = auction.getConditions() == null
        ? List.of()
        : auction.getConditions().stream().map(this::mapCondition).toList();

    return new AuctionDto(
        auction.getId(),
        auction.getCardId(),
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
        auction.getPublisherUser() != null ? auction.getPublisherUser().getRating() : null,
        offerDtos,
        conditionDtos
    );
  }

  private AuctionConditionOutputDto mapCondition(AuctionCondition condition) {
    if (condition instanceof MinimalReputation r) {
      return new AuctionConditionOutputDto("MIN_REPUTATION", r.getReputation(), null);
    }
    if (condition instanceof MinimalCardCount c) {
      return new AuctionConditionOutputDto("MIN_CARD_COUNT", c.getCount(), null);
    }
    if (condition instanceof MinimalExchanges e) {
      return new AuctionConditionOutputDto("MIN_EXCHANGES", e.getQuantity(), null);
    }
    if (condition instanceof MinimalCategory cat) {
      return new AuctionConditionOutputDto("MIN_CATEGORY", null, cat.getCategory());
    }
    return new AuctionConditionOutputDto(condition.getClass().getSimpleName(), null, null);
  }

  public AuctionOfferDto mapOffer(String auctionId, AuctionOffer offer) {
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
        offer.getBidDate(),
        offer.getBidderRating(),
        offer.getBidderAvatarId()
    );
  }

  public List<AuctionDto> mapAuctions(List<Auction> auctions) {
    return auctions.stream().map(this::mapAuction).toList();
  }
}
