package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaFiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaPropuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.output.AlertaSubastaProximaDto;
import org.springframework.stereotype.Component;

@Component
public class AlertVisitor {

  public AlertaDto visit(MissingStickerAlert alerta) {
    Sticker sticker = alerta.getSticker();
    return new AlertaFiguritaFaltanteDto(
        alerta.getId(),
        sticker != null ? sticker.getId() : null,
        sticker != null ? sticker.getNumber() : null,
        sticker != null ? sticker.getDescription() : null,
        sticker != null ? sticker.getCountry() : null,
        sticker != null ? sticker.getTeam() : null,
        sticker != null ? sticker.getCategory() : null
    );
  }

  public AlertaDto visit(AlertProposalReceived alerta) {
    return new AlertaPropuestaRecibidaDto(
        alerta.getId(),
        alerta.getProposal(),
        alerta.getPublication(),
        alerta.getStickerNumbers()
    );
  }

  public AlertaDto visit(AuctionClosingAlert alerta) {
    Sticker sticker = alerta.getSticker();
    return new AlertaSubastaProximaDto(
        alerta.getId(),
        alerta.getAuction(),
        sticker != null ? sticker.getId() : null,
        sticker != null ? sticker.getNumber() : null,
        alerta.getCloseDate()
    );
  }
}
