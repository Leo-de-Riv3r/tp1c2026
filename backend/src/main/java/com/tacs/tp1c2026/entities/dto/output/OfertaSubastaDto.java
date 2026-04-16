package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;

public record OfertaSubastaDto(
    Integer ofertaId,
    Integer subastaId,
    Integer usuarioPostorId,
    Integer cantidadFiguritasOfrecidas,
    List<Integer> idsFiguritasOfrecidas,
    List<ItemOfertaDetalleDto> itemsOfrecidos,
    String estado
) {
  public static record ItemOfertaDetalleDto(
      Integer figuritaId,
      Integer numeroFigurita,
      Integer cantidad
  ) {}
}
