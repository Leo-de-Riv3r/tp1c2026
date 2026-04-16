package com.tacs.tp1c2026.entities.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tacs.tp1c2026.entities.enums.Categoria;

public record AlertaFiguritaFaltanteDto (
    Integer id,
    Integer fromUserId,
    String fromUserName,
    Integer figuritaId,
    Integer figuritaNumero,
    String figuritaJugador,
    String figuritaSeleccion,
    String figuritaEquipo,
    Categoria figuritaCategoria) implements AlertaDto {


    @Override
    @JsonProperty("tipo")
    public String tipo() {
        return "FiguritaFaltante";
    }
}

