package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.dto.statistics.output.TimeseriesDto;
import com.tacs.tp1c2026.entities.statistics.StatsSnapshot;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.ExchangeRepository;
import com.tacs.tp1c2026.repositories.ProposalRepository;
import com.tacs.tp1c2026.repositories.StatsSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

/**
 * Capa 2 del dashboard admin: snapshots diarios de counts de creación. Persiste un doc por día
 * cerrado con {@code auctionsCreated}/{@code proposalsCreated}/{@code exchangesCompleted}, y
 * sirve los endpoints de timeseries combinando snapshots + delta del día en curso (un count live).
 *
 * <p>Los snapshots guardan SÓLO counts de creación (snapshot-safe): no cambian retroactivamente
 * cuando una entidad se cancela o se acepta. Los "actualmente activos" viven en Capa 1 (live).
 *
 * <p>Tres redes de contención para el cron diario:
 * <ol>
 *   <li>{@code @Scheduled} corre a 00:05 todos los días.</li>
 *   <li>{@code @Retryable} reintenta 3 veces con backoff exponencial si Mongo falla.</li>
 *   <li>{@code backfillOnStartup()} recupera snapshots faltantes al levantar el server.</li>
 * </ol>
 */
@Service
public class StatsSnapshotService {

  private static final Logger log = LoggerFactory.getLogger(StatsSnapshotService.class);

  /** Hasta cuántos días hacia atrás se reconstruyen snapshots faltantes (alineado con el TTL). */
  private static final int MAX_BACKFILL_DAYS = 30;

  private final StatsSnapshotRepository snapshotRepository;
  private final AuctionRepository auctionRepository;
  private final ProposalRepository proposalRepository;
  private final ExchangeRepository exchangeRepository;

  public StatsSnapshotService(StatsSnapshotRepository snapshotRepository,
                              AuctionRepository auctionRepository,
                              ProposalRepository proposalRepository,
                              ExchangeRepository exchangeRepository) {
    this.snapshotRepository = snapshotRepository;
    this.auctionRepository = auctionRepository;
    this.proposalRepository = proposalRepository;
    this.exchangeRepository = exchangeRepository;
  }

  // ─── Generación ──────────────────────────────────────────────────────────

  /**
   * Calcula y persiste (upsert) el snapshot del día {@code date}. Idempotente: si ya existe
   * un doc para ese día, lo sobrescribe con los counts recalculados.
   */
  @Retryable(
      retryFor = { OptimisticLockingFailureException.class, DataIntegrityViolationException.class },
      maxAttempts = 3,
      backoff = @Backoff(delay = 60_000, multiplier = 2))
  public StatsSnapshot computeForDate(LocalDate date) {
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();

    long auctions = auctionRepository.countCreatedBetween(start, end);
    long proposals = proposalRepository.countCreatedBetween(start, end);
    long exchanges = exchangeRepository.countCreatedBetween(start, end);

    StatsSnapshot snapshot = snapshotRepository.findByDate(start).orElseGet(StatsSnapshot::new);
    snapshot.setDate(start);
    snapshot.setAuctionsCreated(auctions);
    snapshot.setProposalsCreated(proposals);
    snapshot.setExchangesCompleted(exchanges);
    return snapshotRepository.save(snapshot);
  }

  /** Genera snapshots para todos los días faltantes en {@code [today - MAX_BACKFILL_DAYS, yesterday]}. */
  public int backfillMissing() {
    LocalDate today = LocalDate.now();
    LocalDate from = today.minusDays(MAX_BACKFILL_DAYS);
    int generated = 0;
    for (LocalDate d = from; d.isBefore(today); d = d.plusDays(1)) {
      if (snapshotRepository.findByDate(d.atStartOfDay()).isEmpty()) {
        try {
          computeForDate(d);
          generated++;
        } catch (Exception e) {
          log.warn("Backfill snapshot failed for {}: {}", d, e.getMessage());
        }
      }
    }
    return generated;
  }

  // ─── Servir Capa 2 ───────────────────────────────────────────────────────

  public TimeseriesDto getAuctionsTimeseries(Period period) {
    return buildTimeseries(period, StatsSnapshot::getAuctionsCreated, auctionRepository::countCreatedBetween);
  }

  public TimeseriesDto getProposalsTimeseries(Period period) {
    return buildTimeseries(period, StatsSnapshot::getProposalsCreated, proposalRepository::countCreatedBetween);
  }

  public TimeseriesDto getExchangesTimeseries(Period period) {
    return buildTimeseries(period, StatsSnapshot::getExchangesCompleted, exchangeRepository::countCreatedBetween);
  }

  /**
   * Combina snapshots históricos + delta live del día en curso. El delta evita "snapshot atrasado
   * un día": el FE ve los counts del día actual incluso si el cron de 00:05 aún no corrió.
   */
  private TimeseriesDto buildTimeseries(Period period,
                                        ToLongFunction<StatsSnapshot> extractor,
                                        DateRangeCounter liveCounter) {
    LocalDate today = LocalDate.now();
    LocalDate startDate = today.minusDays(period.days() - 1);

    Map<LocalDate, Long> bySnapshotDate = startDate.isBefore(today)
        ? snapshotRepository.findByDateBetweenOrderByDateAsc(startDate.atStartOfDay(), today.atStartOfDay())
            .stream().collect(Collectors.toMap(s -> s.getDate().toLocalDate(), extractor::applyAsLong))
        : Map.of();

    LocalDateTime startOfToday = today.atStartOfDay();
    LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
    long todayCount = liveCounter.count(startOfToday, startOfTomorrow);

    List<TimeseriesDto.DailyEntry> daily = new ArrayList<>();
    long total = 0;
    for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
      long count = d.equals(today) ? todayCount : bySnapshotDate.getOrDefault(d, 0L);
      daily.add(new TimeseriesDto.DailyEntry(d, count));
      total += count;
    }
    return new TimeseriesDto(period.name().toLowerCase(), total, daily);
  }

  /** Resuelve el snapshot por id si ya existía (para evitar duplicar docs en el upsert). */
  Optional<StatsSnapshot> findSnapshot(LocalDateTime date) {
    return snapshotRepository.findByDate(date);
  }

  public enum Period {
    DAY(1), WEEK(7), MONTH(30);
    private final int days;
    Period(int days) { this.days = days; }
    public int days() { return days; }
  }

  @FunctionalInterface
  private interface DateRangeCounter {
    long count(LocalDateTime start, LocalDateTime end);
  }
}
