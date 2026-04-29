package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.alert.AlertVisitor;
import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
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
  private List<Sticker> stickerNumbers;

  @Override
  public AlertaDto visit(AlertVisitor visitor) {
    return visitor.visit(this);
  }
}
