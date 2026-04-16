package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.Categoria;

public record FiguritaDto(Integer numero, String descripcion, String usuario, String seleccion, String equipo, Categoria categoria) {

}