package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;

public record PropuestaIntercambioDto(
    Integer propuestaId,
    Integer publicacionId,
    Integer numFiguritaPublicada,
    Integer cantidadFiguritasOfrecidas,
    List<Integer> numerosFiguritasOfrecidas,
    Integer usuarioId,
    String estado
) {
}
