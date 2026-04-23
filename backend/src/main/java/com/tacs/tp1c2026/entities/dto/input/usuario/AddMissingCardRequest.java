package com.tacs.tp1c2026.entities.dto.input.usuario;

import jakarta.validation.constraints.NotNull;

public record AddMissingCardRequest(
    @NotNull(message = "figuritaId es obligatorio")
    Integer figuritaId
) {}