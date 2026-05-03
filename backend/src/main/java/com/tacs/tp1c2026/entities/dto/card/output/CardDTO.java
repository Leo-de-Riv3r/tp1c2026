package com.tacs.tp1c2026.entities.dto.card.output;

import com.tacs.tp1c2026.entities.enums.Category;

public record CardDTO(
    Integer number,
    String player,
    String country,
    String team,
    String description,
    Category category
) {}
