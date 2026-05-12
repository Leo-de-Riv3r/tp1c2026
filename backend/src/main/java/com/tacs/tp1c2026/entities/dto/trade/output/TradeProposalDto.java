package com.tacs.tp1c2026.entities.dto.trade.output;

import com.tacs.tp1c2026.entities.dto.card.output.CardDTO;
import com.tacs.tp1c2026.entities.dto.user.output.UserDto;
import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TradeProposalDto(
    String id,
    String publicationId,
    List<String> cardIds,
    List<CardDTO> cards,
    Integer requestedCount,
    String proposerUserId,
    UserDto receiver,
    TradeProposalStatus status,
    LocalDateTime creationDate
) {}
