package com.tacs.tp1c2026.entities.dto.input.usuario;

import jakarta.validation.constraints.NotNull;

public record AddToCollectionRequest(
    @NotNull(message = "figuritaId es obligatorio")
    Integer figuritaId
) {}