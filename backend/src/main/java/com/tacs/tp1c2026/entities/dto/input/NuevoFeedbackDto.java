package com.tacs.tp1c2026.entities.dto.input;


public record NuevoFeedbackDto(
    Integer calificacion,
    Integer publicacionId,
    String comentario
) {}

