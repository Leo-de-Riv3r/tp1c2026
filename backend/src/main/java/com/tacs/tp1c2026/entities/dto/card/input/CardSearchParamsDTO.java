package com.tacs.tp1c2026.entities.dto.card.input;

import com.tacs.tp1c2026.entities.enums.Category;

/**
 * Parameters used when searching for a sticker. All fields are optional; the caller may
 * provide any subset to filter the search.
 *
 * Fields (in English):
 * - number: sticker number
 * - playerOrDescription: player name or free-text description to match against
 * - country: sticker selection / country
 * - team: sticker team
 * - category: sticker category (COMMON, EPIC, LEGENDARY)
 */
public record CardSearchParamsDTO(
    Integer number,
    String description,
    String player,
    String country,
    String team,
    Category category
) {}

