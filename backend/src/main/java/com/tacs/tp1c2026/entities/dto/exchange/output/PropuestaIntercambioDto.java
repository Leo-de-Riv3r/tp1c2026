package com.tacs.tp1c2026.entities.dto.exchange.output;

import java.util.List;
import lombok.Data;

@Data
public class PropuestaIntercambioDto {
  private Integer propuestaId; // id de la propuesta
  private Integer publicacionId; // id de la publicacion a la que pertenece la propuesta
  private Integer numFiguritaPublicada; // id (num) de la figurita de la publicacion a la que pertenece la propuesta
  private Integer cantidadFiguritasOfrecidas; // cantidad de figuritas que ofrece la propuesta
  private List<Integer> numerosFiguritasOfrecidas; // numeros de las figuritas que ofrece la propuesta
  private Integer usuarioId; // id del usuario que hace la propuesta
  private String estado; // estado de la propuesta
}
