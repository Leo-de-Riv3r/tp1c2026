package com.tacs.tp1c2026.entities.dto.output.auction;

import java.util.List;

public record AuctionOfferDto(
        Integer ofertaId,
        Integer subastaId,
        Integer usuarioPostorId,
        Integer cantidadFiguritasOfrecidas,
        List<Integer> idsFiguritasOfrecidas,
        List<ItemOfertaDetalleDto> itemsOfrecidos,
        String estado
) {
  public record ItemOfertaDetalleDto(
          Integer figuritaId,
          Integer numeroFigurita,
          Integer cantidad
  ) {}
}

