package com.tacs.tp1c2026.entities.dto.output;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SubastaDto {
  private Integer subastaId;
  private Integer usuarioPublicanteId;
  private Integer numFiguritaPublicada;
  private Integer cantidadMinFiguritas;
  private LocalDateTime fechaCreacion;
  private LocalDateTime fechaCierre;
  private String estado;
}
