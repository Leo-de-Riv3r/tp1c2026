package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.statistics.StatsSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StatsSnapshotRepository extends Repository<StatsSnapshot, String> {

  Optional<StatsSnapshot> findByDate(LocalDateTime date);

  List<StatsSnapshot> findByDateBetweenOrderByDateAsc(LocalDateTime from, LocalDateTime to);
}
