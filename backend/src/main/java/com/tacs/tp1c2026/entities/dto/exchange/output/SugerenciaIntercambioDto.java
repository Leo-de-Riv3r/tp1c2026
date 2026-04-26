package com.tacs.tp1c2026.entities.dto.exchange.output;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import com.tacs.tp1c2026.entities.dto.card.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.card.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.entities.dto.user.output.UsuarioBasicoDto;

@Data
@AllArgsConstructor
public class SugerenciaIntercambioDto {
  private UsuarioBasicoDto usuario;
  private List<FiguritaRepetidaDto> figuritasQueTiene;
  private List<FiguritaFaltanteDto> figuritasQueFaltan;
}
