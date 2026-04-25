package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.Category;

public record FiguritaDto(
    Integer number,
    String description,
    String player,
    String country,
    String team,
    Category category
) {}
