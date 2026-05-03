package com.tacs.tp1c2026.entities.dto.user.input;

import jakarta.validation.constraints.NotBlank;

public record AddMissingCardRequest(
    @NotBlank(message = "cardId es obligatorio")
    String cardId
) {}