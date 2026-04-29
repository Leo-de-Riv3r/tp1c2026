package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.dto.input.MissingCardDto;
import com.tacs.tp1c2026.entities.dto.input.RegisterRepeatedCardDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SugerenciaIntercambioDto {
  private UsuarioBasicoDto usuario;
  private List<RegisterRepeatedCardDto> figuritasQueTiene;
  private List<MissingCardDto> figuritasQueFaltan;
}
