package com.tacs.tp1c2026.entities.dto.auction.output;

import com.tacs.tp1c2026.entities.enums.Category;

public record AuctionConditionOutputDto(
    String filterName,
    Integer quantity,
    Category value
) {}
