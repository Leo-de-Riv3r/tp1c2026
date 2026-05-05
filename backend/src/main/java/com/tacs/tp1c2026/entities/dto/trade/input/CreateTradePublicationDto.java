package com.tacs.tp1c2026.entities.dto.trade.input;

/**
 * DTO for creating a trade publication.
 * {@code quantity} es el initialCount al publicarse: cuántas unidades del card se ofrecen.
 */
public record CreateTradePublicationDto(
    String cardId,
    Integer quantity
) {}

