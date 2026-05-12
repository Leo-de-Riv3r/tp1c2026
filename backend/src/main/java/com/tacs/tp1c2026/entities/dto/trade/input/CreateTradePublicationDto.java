package com.tacs.tp1c2026.entities.dto.trade.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a trade publication.
 * {@code quantity} es el initialCount al publicarse: cuántas unidades del card se ofrecen.
 */
public record CreateTradePublicationDto(
    @NotBlank(message = "cardId es requerido")
    String cardId,

    @NotNull(message = "quantity es requerido")
    @Min(value = 1, message = "quantity debe ser mayor o igual a 1")
    Integer quantity
) {}
