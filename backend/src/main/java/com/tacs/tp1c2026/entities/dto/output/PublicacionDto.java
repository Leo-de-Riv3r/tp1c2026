package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.Categoria;

public record PublicacionDto(
    Integer id,
    Integer numero,
    String descripcion,
    String jugador,
    String seleccion,
    String equipo,
    Categoria categoria,
    Integer cantidad
) {
}
