package com.tacs.tp1c2026.entities.dto.statistics.output;

/**
 * Entry of the "most wanted cards" list — each element describes a card and how many
 * users marked it as missing in the considered time window
 */
public record MostWantedCardEntry(
    String cardId,
    Integer cardNumber,
    String cardDescription,
    long userCount,
    int periodDays
) {}
