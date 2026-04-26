package com.tacs.tp1c2026.entities.dto.auction.output;

import lombok.Data;

import java.util.List;

@Data
public class OfertaSubastaDto {
  private Integer ofertaId;
  private Integer subastaId;
  private Integer usuarioPostorId;
  private Integer cantidadFiguritasOfrecidas;
  private List<Integer> idsFiguritasOfrecidas;
//  private List<Integer> numerosFiguritasOfrecidas;
  private List<ItemOfertaDetalleDto> itemsOfrecidos;
  private String estado;

  @Data
  public static class ItemOfertaDetalleDto {
    private Integer figuritaId;
    private Integer numeroFigurita;
    private Integer cantidad;
  }
}
