package com.tacs.tp1c2026.entities.statistics;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Snapshot diario de métricas de creación (Capa 2 del dashboard admin). Se persiste un doc por
 * día CERRADO con los counts del día. El cron {@code StatsSnapshotGenerator} lo genera a las 00:05;
 * el endpoint de timeseries combina N snapshots + el delta del día en curso.
 *
 * <p>Sólo guarda conteos de CREACIÓN (snapshot-safe): {@code creationDate} no muta retroactivamente,
 * así que las cancelaciones posteriores no rompen el histórico.
 *
 * <p>{@code @Indexed(expireAfterSeconds = 30 días)} aplica TTL nativo: Mongo borra docs >30 días
 * sin intervención. Sin índice único — el service hace upsert por fecha (idempotente si el cron
 * corre dos veces el mismo día).
 */
@Document(collection = "stats_snapshots")
@TypeAlias("stats_snapshot")
@Getter
@Setter
public class StatsSnapshot {

  /** Retención del snapshot. Alineado con la ventana máxima del backfill on-startup. */
  public static final String TTL = "30d";

  @Id
  private String id;

  /** Medianoche del día cerrado (e.g. 2026-06-14T00:00:00). Usado para TTL y para matchear el período. */
  @Indexed(expireAfter = TTL)
  private LocalDateTime date;

  private long auctionsCreated;
  private long proposalsCreated;
  private long exchangesCompleted;

  public StatsSnapshot() {
    // Spring Data + upsert path: el service hace findByDate().orElseGet(StatsSnapshot::new)
  }

  public StatsSnapshot(LocalDateTime date, long auctionsCreated, long proposalsCreated, long exchangesCompleted) {
    this.date = date;
    this.auctionsCreated = auctionsCreated;
    this.proposalsCreated = proposalsCreated;
    this.exchangesCompleted = exchangesCompleted;
  }
}
