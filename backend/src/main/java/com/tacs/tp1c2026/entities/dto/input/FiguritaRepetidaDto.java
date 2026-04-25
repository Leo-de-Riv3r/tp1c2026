package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.enums.ParticipationType;

public record FiguritaRepetidaDto(
    Integer number,
    String player,
    String country,
    String team,
    String description,
    Category category,
    Integer amount,
    ParticipationType participationType
) {}
