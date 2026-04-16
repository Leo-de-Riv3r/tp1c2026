package com.tacs.tp1c2026.entities.dto.output;

import java.time.LocalDateTime;

public record UsuarioDto(Integer id, String nombre, LocalDateTime fechaAlta) {
}
