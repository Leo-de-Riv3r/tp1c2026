package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.Category;

public record PublicacionDto(
    Integer id,
    Integer stickerNumber,
    String description,
    String player,
    String country,
    String team,
    Category category,
    Integer availableAmount
) {}
