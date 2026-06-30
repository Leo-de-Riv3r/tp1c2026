package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.match.UpcomingMatchesCache;

/** Cache de próximos partidos (documento único). {@code findById}/{@code save} vienen de la base. */
public interface UpcomingMatchesRepository extends Repository<UpcomingMatchesCache, String> {
}
