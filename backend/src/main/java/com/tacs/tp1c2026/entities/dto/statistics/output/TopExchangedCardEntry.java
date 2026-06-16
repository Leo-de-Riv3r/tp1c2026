package com.tacs.tp1c2026.entities.dto.statistics.output;

/**
 * Entry del top de cartas más intercambiadas en una ventana de tiempo. {@code occurrences}
 * cuenta cada aparición en {@code cardsFromA} o {@code cardsFromB} de un Exchange (si una card
 * participó en N intercambios, suma N).
 */
public record TopExchangedCardEntry(
    String cardId,
    Integer cardNumber,
    String cardDescription,
    long occurrences,
    int periodDays
) {}
