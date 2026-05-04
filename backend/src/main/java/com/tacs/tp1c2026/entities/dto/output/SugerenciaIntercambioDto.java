package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.dto.input.user.MissingCardDto;
import com.tacs.tp1c2026.entities.dto.input.user.RegisterRepeatedCardDto;
import com.tacs.tp1c2026.entities.dto.output.user.UsuarioBasicoDto;
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
