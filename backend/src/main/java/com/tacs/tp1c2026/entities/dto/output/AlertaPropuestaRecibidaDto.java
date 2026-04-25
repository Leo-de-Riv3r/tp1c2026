package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.Sticker;
import com.tacs.tp1c2026.entities.TradeProposal;
import com.tacs.tp1c2026.entities.TradePublication;

import java.util.List;

public class AlertaPropuestaRecibidaDto extends AlertaDto {

    private final TradeProposal proposal;
  private final TradePublication publication;
  private final List<Sticker> stickerNumbers;

  public AlertaPropuestaRecibidaDto(
      Integer id,
      TradeProposal proposal,
      TradePublication publication,
      List<Sticker> stickerNumbers) {
    super(id, "PROPUESTA_RECIBIDA");
    this.proposal = proposal;
    this.publication = publication;
    this.stickerNumbers = stickerNumbers;
  }

}
