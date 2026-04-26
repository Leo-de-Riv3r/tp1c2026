package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.Category;

public record StickerDTO(
    Integer number,
    String player,
    String country,
    String team,
    String description,
    Category category
) {}
