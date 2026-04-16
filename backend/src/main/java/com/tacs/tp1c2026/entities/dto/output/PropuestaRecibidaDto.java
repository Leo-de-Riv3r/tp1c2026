package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;

public record PropuestaRecibidaDto(
    Integer id,
    List<FiguritaDto> figuritas,
    UsuarioDto usuario
) {
}
