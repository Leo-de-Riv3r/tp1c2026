package com.tacs.tp1c2026.entities.dto.card.input;

import com.tacs.tp1c2026.entities.enums.Category;

/**
 * Parámetros usados al buscar una figurita. Todos los campos son opcionales; el llamador puede
 * proporcionar cualquier subconjunto para filtrar la búsqueda.
 *
 * Campos:
 * - number: número de la figurita
 * - playerOrDescription: nombre del jugador o descripción de texto libre contra la cual matchear
 * - country: selección / país de la figurita
 * - team: equipo de la figurita
 * - category: categoría de la figurita (COMMON, EPIC, LEGENDARY)
 */
public record CardSearchParamsDTO(
    Integer number,
    String description,
    String player,
    String country,
    String team,
    Category category
) {}

