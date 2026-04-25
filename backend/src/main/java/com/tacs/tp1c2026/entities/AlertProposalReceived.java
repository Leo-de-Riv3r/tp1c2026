package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import java.util.List;

import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


@TypeAlias("proposal_received")
@Getter
public class AlertProposalReceived extends Alert {

  @DocumentReference
  private TradeProposal proposal;

  @DocumentReference
  private TradePublication publication;

  @DocumentReference
  private List<Sticker> stickerNumbers;

  @Override
  public AlertaDto visit(AlertVisitor visitor) {
    return visitor.visit(this);
  }
}
