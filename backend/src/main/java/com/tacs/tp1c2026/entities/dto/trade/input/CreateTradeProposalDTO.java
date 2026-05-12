package com.tacs.tp1c2026.entities.dto.trade.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO para crear una propuesta sobre una publicación.
 * - {@code cardIds}: lista de cards ofrecidas (con repetidos según cantidad).
 *   No puede estar vacía: una propuesta sin figuritas no tiene sentido.
 * - {@code requestedCount}: cantidad de unidades del card publicado que pide el bidder.
 *   Tiene que cumplir 1 ≤ requestedCount ≤ publication.remainingCount.
 */
public record CreateTradeProposalDTO(
    @NotBlank(message = "publicationId es requerido")
    String publicationId,

    @NotEmpty(message = "cardIds no puede estar vacío")
    List<String> cardIds,

    @NotNull(message = "requestedCount es requerido")
    @Min(value = 1, message = "requestedCount debe ser mayor o igual a 1")
    Integer requestedCount
) {}
