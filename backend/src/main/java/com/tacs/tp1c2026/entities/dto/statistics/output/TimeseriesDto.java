package com.tacs.tp1c2026.entities.dto.statistics.output;

import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de los endpoints de Capa 2 (counts por período). {@code total} es la suma de
 * los días del período (incluido el día en curso); {@code daily} desglosa por fecha para
 * que el FE pueda graficar la curva.
 *
 * <p>Para {@code period=day} la lista {@code daily} tiene un único elemento (el día actual).
 */
public record TimeseriesDto(
    String period,
    long total,
    List<DailyEntry> daily
) {
  public record DailyEntry(LocalDate date, long count) {}
}
