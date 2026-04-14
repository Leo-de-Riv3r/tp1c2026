package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;
import lombok.Setter;

@Setter
public class PropuestaRecibidaDto {
  private Integer id;
  private List<FiguritaDto> figuritas;
  private UsuarioDto usuario;
}
