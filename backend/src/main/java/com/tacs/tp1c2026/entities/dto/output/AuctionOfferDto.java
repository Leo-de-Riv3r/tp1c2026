package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;

public record AuctionOfferDto(
        Integer ofertaId,
        Integer subastaId,
        Integer usuarioPostorId,
        Integer cantidadFiguritasOfrecidas,
        List<Integer> idsFiguritasOfrecidas,
        List<OfferItemDetailDto> itemsOfrecidos,
        String estado
) {
  public record OfferItemDetailDto(
          Integer figuritaId,
          Integer numeroFigurita,
          Integer cantidad
  ) {}
}

