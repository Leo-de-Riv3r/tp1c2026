package com.tacs.tp1c2026.entities.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AlertaPropuestaRecibidaDto(
    Integer id,
    Integer fromUserId,
    String fromUserName,
    Integer propuestaId,
    Integer publicacionId,
    List<Integer> figuritaNumeros
) implements AlertaDto {

    @Override
    @JsonProperty("tipo")
    public String tipo() {
        return "PropuestaRecibida";
    }

}