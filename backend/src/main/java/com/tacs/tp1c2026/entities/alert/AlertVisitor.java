package com.tacs.tp1c2026.entities.alert;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.alert.output.AlertDto;
import com.tacs.tp1c2026.entities.dto.alert.output.AlertaFiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.alert.output.AlertaPorpuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.alert.output.AlertaSubastaProximaDto;
import com.tacs.tp1c2026.entities.user.embedded.AuctionClosingAlert;
import com.tacs.tp1c2026.entities.user.embedded.MissingCardAlert;
import com.tacs.tp1c2026.entities.user.embedded.ProposalReceivedAlert;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlertVisitor {

  public AlertDto visit(MissingCardAlert alert) {
    Card card = alert.getCard();
    return new AlertaFiguritaFaltanteDto(
        alert.getId(),
        null,
        null,
        card != null ? card.getId() : null,
        card != null ? card.getNumber() : null,
        card != null ? card.getPlayer() : null,
        card != null ? card.getCountry() : null,
        card != null ? card.getTeam() : null,
        card != null ? card.getCategory() : null
    );
  }

  public AlertDto visit(ProposalReceivedAlert alerta) {
    var proposal = alerta.getProposal();
    List<Integer> cardNumbers = alerta.getCardNumbers() != null
        ? alerta.getCardNumbers().stream().map(Card::getNumber).toList()
        : null;
    return new AlertaPorpuestaRecibidaDto(
        alerta.getId(),
        proposal != null ? proposal.getProposerUser().getId() : null,
        proposal != null ? proposal.getProposerUser().getName() : null,
        proposal != null ? proposal.getId() : null,
        alerta.getPublication() != null ? alerta.getPublication().getId() : null,
        cardNumbers
    );
  }

  public AlertDto visit(AuctionClosingAlert alerta) {
    Card card = alerta.getCard();
    return new AlertaSubastaProximaDto(
        alerta.getId(),
        alerta.getAuction() != null ? alerta.getAuction().getId() : null,
        card != null ? card.getId() : null,
        card != null ? card.getNumber() : null,
        alerta.getCloseDate()
    );
  }
}
