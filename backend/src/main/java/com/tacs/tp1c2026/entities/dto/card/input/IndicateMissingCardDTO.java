package com.tacs.tp1c2026.entities.dto.card.input;

/**
 * DTO usado cuando un usuario indica una figurita faltante. Contiene el identificador de la figurita.
 */
public record IndicateMissingCardDTO(
    String cardId
) {}

