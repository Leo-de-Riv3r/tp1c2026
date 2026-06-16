package com.tacs.tp1c2026.entities.dto.user.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddToCollectionRequest(
    @NotBlank(message = "cardId es obligatorio")
    String cardId,

    @Min(value = 1, message = "quantity debe ser al menos 1")
    @Max(value = 100, message = "quantity no puede superar 100")
    Integer quantity
) {
    // quantity es opcional: si el cliente no lo manda, se asume 1 (compat con el add de a una).
    public AddToCollectionRequest {
        if (quantity == null) quantity = 1;
    }
}
