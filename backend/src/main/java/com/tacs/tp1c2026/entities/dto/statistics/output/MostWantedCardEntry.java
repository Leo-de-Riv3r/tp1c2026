package com.tacs.tp1c2026.entities.dto.statistics.output;

/**
 * Entrada de la lista de "figuritas más buscadas" — cada elemento describe una figurita y cuántos
 * usuarios la marcaron como faltante en la ventana de tiempo considerada.
 */
public record MostWantedCardEntry(
    String cardId,
    Integer cardNumber,
    String cardDescription,
    long userCount,
    int periodDays
) {}
