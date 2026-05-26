package com.tacs.tp1c2026.entities.dto.statistics.output;

/**
 * Entrada de la lista "cartas más buscadas" — cada elemento describe una card y cuántos
 * users la marcaron como faltante en la ventana de días considerada
 */
public record MostWantedCardEntry(
    String cardId,
    Integer cardNumber,
    String cardDescription,
    long userCount,
    int periodDays
) {}
