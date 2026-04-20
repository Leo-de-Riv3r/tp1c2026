package com.tacs.tp1c2026.entities.dto.input.usuario;

import jakarta.validation.constraints.NotBlank;

public record AddToCollectionRequest(
    @NotBlank(message = "figuritaId es obligatorio")
    String figuritaId
) {}