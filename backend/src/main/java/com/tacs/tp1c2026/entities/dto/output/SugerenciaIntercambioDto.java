package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;

@Data
@AllArgsConstructor
public class SugerenciaIntercambioDto {
  private UsuarioBasicoDto usuario;
  private List<FiguritaRepetidaDto> figuritasQueTiene;
  private List<FiguritaFaltanteDto> figuritasQueFaltan;
}
