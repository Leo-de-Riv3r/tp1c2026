package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.Category;

public record FiguritaRepetidaDto(
    StickerDTO sticker,
    Integer amount
) {}
