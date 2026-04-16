package com.tacs.tp1c2026.entities.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

public record AlertaSubastaProximaDto(
    Integer id,
    Integer subastaId,
    Integer figuritaId,
    Integer figuritaNumero,
    LocalDateTime fechaCierre
) implements AlertaDto {
    @Override
    @JsonProperty("tipo")
    public String tipo() {
        return "SubastaProxima";
    }
}