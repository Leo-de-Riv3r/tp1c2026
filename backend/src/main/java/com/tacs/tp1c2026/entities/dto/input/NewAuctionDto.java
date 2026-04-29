package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NewAuctionDto {
  @NotNull(message = "El numero de la figurita es requerido")
  private Integer numFiguritaPublicada;
  @NotNull(message = "La duracion de la subasta es requerida")
  @Min(value = 8, message = "La duracion de la subasta debe ser mayor a 8 horas")
  private Integer durationHours;
  @NotNull(message = "La cantidad minima de figuritas es requerida")
  private Integer minCardsRequired;
}
