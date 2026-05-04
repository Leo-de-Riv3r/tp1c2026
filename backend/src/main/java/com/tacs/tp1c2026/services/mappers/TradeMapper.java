package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.trade.output.TradeProposalDto;
import com.tacs.tp1c2026.entities.dto.trade.output.TradePublicationDto;
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
        publication.getQuantity(),
        publication.getCardNumber(),
        publication.getStatus(),
        publication.getCardDescription(),
        publication.getCardCountry(),
        publication.getCardTeam(),
        publication.getCardCategory()
    );
  }

  public List<TradePublicationDto> mapPublications(List<TradePublication> publications) {
    return publications.stream().map(this::mapPublication).toList();
  }

  public TradeProposalDto mapProposal(TradeProposal proposal) {
    List<String> cardIds = proposal.getCards().stream()
        .map(Card::getId)
        .toList();
    return new TradeProposalDto(
        proposal.getId(),
        proposal.getPublication() != null ? proposal.getPublication().getId() : null,
        cardIds,
        proposal.getProposerUser() != null ? proposal.getProposerUser().getId() : null,
        proposal.getStatus(),
        proposal.getCreationDate()
    );
  }

  public List<TradeProposalDto> mapProposals(List<TradeProposal> proposals) {
    return proposals.stream().map(this::mapProposal).toList();
  }
}
