package com.tacs.tp1c2026.entities.dto.statistics.output;

/**
 * Counts puntuales del sistema (Capa 1 del dashboard admin). Snapshot del estado actual:
 * cada campo es un {@code count()} sobre su colección, ejecutado live (no cacheado).
 */
public record OverviewDto(
    long totalUsers,
    long activeAuctions,
    long activePublications,
    long totalExchanges
) {}
