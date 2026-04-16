package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.Categoria;

public record FiguritaFaltanteDto (
   Integer numero,
   String jugador,
   String seleccion,
   String equipo,
   String descripcion,
   Categoria categoria
) {}