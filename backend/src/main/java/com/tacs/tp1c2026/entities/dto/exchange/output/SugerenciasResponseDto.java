package com.tacs.tp1c2026.entities.dto.exchange.output;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SugerenciasResponseDto {
  private List<SugerenciaIntercambioDto> sugerencias;
}
