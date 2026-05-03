package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.alert.AlertVisitor;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.alert.output.AlertDto;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.List;


@TypeAlias("proposal_received")
@Getter
public class ProposalReceivedAlert extends Alert {

  @DocumentReference
  private TradeProposal proposal;

  @DocumentReference
  private TradePublication publication;

  @DocumentReference
  private List<Card> cardNumbers;

  @Override
  public AlertDto visit(AlertVisitor visitor) {
    return visitor.visit(this);
  }
}
