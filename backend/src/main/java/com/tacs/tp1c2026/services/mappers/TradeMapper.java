package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.card.output.CardDTO;
import com.tacs.tp1c2026.entities.dto.mappers.CardMapper;
import com.tacs.tp1c2026.entities.dto.trade.output.TradeProposalDto;
import com.tacs.tp1c2026.entities.dto.trade.output.TradePublicationDto;
import com.tacs.tp1c2026.entities.dto.user.output.UserDto;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper para convertir entidades de intercambio a sus representaciones DTO.
 */
@Component
public class TradeMapper {

  public TradePublicationDto mapPublication(TradePublication publication) {
    return new TradePublicationDto(
        publication.getId(),
        publication.getInitialCount(),
        publication.getRemainingCount(),
        publication.getCardNumber(),
        publication.getStatus(),
        publication.getCardDescription(),
        publication.getCardCountry(),
        publication.getCardTeam(),
        publication.getCardCategory(),
        publication.getPublisherUser() != null ? publication.getPublisherUser().getId() : null,
        publication.getPublisherName(),
        publication.getPublisherAvatarId()
    );
  }

  public List<TradePublicationDto> mapPublications(List<TradePublication> publications) {
    return publications.stream().map(this::mapPublication).toList();
  }

  public TradeProposalDto mapProposal(TradeProposal proposal) {
    List<String> cardIds = proposal.getCards().stream()
        .map(Card::getId)
        .toList();
    List<CardDTO> cards = proposal.getCards().stream()
        .map(CardMapper::toDto)
        .toList();
    return new TradeProposalDto(
        proposal.getId(),
        proposal.getPublication() != null ? proposal.getPublication().getId() : null,
        cardIds,
        cards,
        proposal.getRequestedCount(),
        proposal.getProposerUser() != null ? proposal.getProposerUser().getId() : null,
        proposal.getReceiver() != null ? UserDto.from(proposal.getReceiver()) : null,
        proposal.getStatus(),
        proposal.getCreationDate()
    );
  }

  public List<TradeProposalDto> mapProposals(List<TradeProposal> proposals) {
    return proposals.stream().map(this::mapProposal).toList();
  }
}
