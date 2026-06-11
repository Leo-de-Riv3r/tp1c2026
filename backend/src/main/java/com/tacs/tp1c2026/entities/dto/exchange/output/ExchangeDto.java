package com.tacs.tp1c2026.entities.dto.exchange.output;

import com.tacs.tp1c2026.entities.exchange.embedded.CardSnapshot;
import com.tacs.tp1c2026.entities.exchange.embedded.ExchangeOrigin;
import com.tacs.tp1c2026.entities.exchange.embedded.Feedback;
import com.tacs.tp1c2026.entities.exchange.embedded.UserSnapshot;

import java.time.LocalDateTime;
import java.util.List;

public record ExchangeDto(
    String id,
    ExchangeOrigin origin,
    UserSnapshot userA,
    UserSnapshot userB,
    List<CardSnapshot> cardsFromA,
    List<CardSnapshot> cardsFromB,
    LocalDateTime createdAt,
    Feedback feedbackFromA,
    Feedback feedbackFromB
) {}
