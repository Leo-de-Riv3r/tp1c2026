package com.tacs.tp1c2026.entities.dto.trade.output;

import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TradeProposalDto(
    String id,
    String publicationId,
    List<String> cardIds,
    Integer requestedCount,
    String proposerUserId,
    TradeProposalStatus status,
    LocalDateTime creationDate
) {}
