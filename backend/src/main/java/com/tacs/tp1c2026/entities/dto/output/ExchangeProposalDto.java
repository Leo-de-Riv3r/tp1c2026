package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.Card;
import com.tacs.tp1c2026.entities.enums.ProposalState;
import java.time.LocalDateTime;
import java.util.List;

public record ExchangeProposalDto(
    String id,
    String publicacionId,
    List<Card> cards,
    String userId,
    ProposalState state,
    LocalDateTime creationDate
) {}
